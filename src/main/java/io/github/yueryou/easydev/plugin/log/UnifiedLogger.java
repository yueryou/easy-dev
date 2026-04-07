package io.github.yueryou.easydev.plugin.log;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 统一日志记录器
 * - 整合 MCP 日志和全局操作日志
 * - 滚动日志文件，按大小切割
 * - 最大总大小 200MB
 * - 异步写入，不阻塞主线程
 * - 特别标记异常和 null 数据
 *
 * @author yuex
 * @date 2026/04/03
 */
public class UnifiedLogger {

    private static final Logger IDE_LOG = Logger.getInstance(UnifiedLogger.class);

    // 日志配置
    private static final long MAX_LOG_SIZE = 200L * 1024 * 1024; // 200MB
    private static final long MAX_SINGLE_FILE_SIZE = 50L * 1024 * 1024; // 50MB per file
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // 日志目录和文件
    private static final String LOG_DIR_NAME = "easy-deploy-logs";
    private static final String LOG_FILE_PREFIX = "easy-deploy";
    private static final String LOG_FILE_SUFFIX = ".log";

    // 单例
    private static volatile UnifiedLogger instance;

    // 状态
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean capturing = new AtomicBoolean(false);
    private final AtomicLong currentFileSize = new AtomicLong(0);
    private final ReentrantLock writeLock = new ReentrantLock();

    // 日志文件
    private final Path logDir;
    private volatile Path currentLogFile;

    // 异步写入
    private final ExecutorService writerExecutor;
    private final BlockingQueue<LogEntry> logQueue;
    private volatile boolean running = true;

    // 日志级别
    public enum Level {
        DEBUG("DEBUG", false),
        INFO("INFO", false),
        WARN("WARN", false),
        ERROR("ERROR", false),
        EXCEPTION("EXCEPTION", true),    // 异常专用
        NULL_DATA("NULL", true),         // null 数据专用
        ACTION("ACTION", false),         // Action 执行
        BUTTON("BUTTON", false),         // 按钮点击
        FLOW("FLOW", false),             // 关键流程
        SSH("SSH", false),               // SSH 操作
        FILE_TRANSFER("FILE", false),    // 文件传输
        TOOL("TOOL", false);             // MCP 工具调用

        private final String name;
        private final boolean highlight;

        Level(String name, boolean highlight) {
            this.name = name;
            this.highlight = highlight;
        }

        public String getName() { return name; }
        public boolean isHighlight() { return highlight; }
    }

    // 日志条目
    private static class LogEntry {
        final Level level;
        final String category;
        final String action;
        final String message;
        final Throwable throwable;
        final LocalDateTime timestamp;

        LogEntry(Level level, String category, String action, String message, Throwable throwable) {
            this.level = level;
            this.category = category;
            this.action = action;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = LocalDateTime.now();
        }
    }

    private UnifiedLogger() {
        // 初始化日志目录
        String userHome = System.getProperty("user.home");
        this.logDir = Paths.get(userHome, ".easy-deploy", LOG_DIR_NAME);

        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            IDE_LOG.error("Failed to create log directory: " + logDir, e);
        }

        // 初始化异步写入
        this.logQueue = new LinkedBlockingQueue<>(50000);
        this.writerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Easy-Deploy-Logger");
            t.setDaemon(true);
            return t;
        });

        // 启动写入线程
        startWriterThread();

        IDE_LOG.info("UnifiedLogger initialized, log dir: " + logDir);
    }

    public static UnifiedLogger getInstance() {
        if (instance == null) {
            synchronized (UnifiedLogger.class) {
                if (instance == null) {
                    instance = new UnifiedLogger();
                }
            }
        }
        return instance;
    }

    // ==================== 开关控制 ====================

    /**
     * 启用日志（MCP 日志）
     */
    public void enable() {
        if (enabled.compareAndSet(false, true)) {
            initLogFile();
            log(Level.INFO, "SYSTEM", "INIT", "MCP logging enabled");
        }
    }

    /**
     * 禁用日志
     */
    public void disable() {
        if (enabled.compareAndSet(true, false)) {
            log(Level.INFO, "SYSTEM", "INIT", "MCP logging disabled");
        }
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 开始全局抓取
     */
    public void startCapture() {
        if (capturing.compareAndSet(false, true)) {
            if (!enabled.get()) {
                enabled.set(true);
                initLogFile();
            }
            log(Level.INFO, "SYSTEM", "CAPTURE", "Global capture started");
            IDE_LOG.info("Global capture started");
        }
    }

    /**
     * 停止全局抓取
     */
    public void stopCapture() {
        if (capturing.compareAndSet(true, false)) {
            log(Level.INFO, "SYSTEM", "CAPTURE", "Global capture stopped");
            IDE_LOG.info("Global capture stopped");
        }
    }

    /**
     * 是否正在抓取
     */
    public boolean isCapturing() {
        return capturing.get();
    }

    /**
     * 切换抓取状态
     */
    public boolean toggleCapture() {
        if (isCapturing()) {
            stopCapture();
            return false;
        } else {
            startCapture();
            return true;
        }
    }

    // ==================== 日志记录方法 ====================

    /**
     * 核心日志方法
     */
    public void log(Level level, String category, String action, String message) {
        if (!enabled.get()) return;
        enqueueLog(level, category, action, message, null);
    }

    /**
     * 带异常的日志
     */
    public void logException(String category, String action, String message, Throwable throwable) {
        if (!enabled.get()) return;
        enqueueLog(Level.EXCEPTION, category, action, message, throwable);
        IDE_LOG.error("[" + category + "] " + message, throwable);
    }

    /**
     * null 数据日志
     */
    public void logNullData(String category, String fieldName, String context) {
        if (!enabled.get()) return;
        String message = "Field is null: " + fieldName + " | Context: " + context;
        enqueueLog(Level.NULL_DATA, category, "NULL_CHECK", message, null);
        IDE_LOG.warn("[" + category + "] " + message);
    }

    // ==================== 便捷方法 - MCP 日志 ====================

    public void toolCall(String toolName, String params) {
        String maskedParams = SensitiveDataMasker.maskJson(params);
        log(Level.TOOL, "MCP", toolName, "Params: " + truncate(maskedParams, 500));
    }

    public void toolResult(String toolName, boolean success, String result, long durationMs) {
        String status = success ? "SUCCESS" : "FAILED";
        String maskedResult = SensitiveDataMasker.maskJson(result);
        log(Level.TOOL, "MCP", toolName,
                String.format("[%s] %dms | Result: %s", status, durationMs, truncate(maskedResult, 500)));
    }

    public void debug(String category, String message) {
        log(Level.DEBUG, category, "DEBUG", message);
    }

    public void info(String category, String message) {
        log(Level.INFO, category, "INFO", message);
    }

    public void warn(String category, String message) {
        log(Level.WARN, category, "WARN", message);
    }

    public void error(String category, String message) {
        log(Level.ERROR, category, "ERROR", message);
    }

    // ==================== 便捷方法 - 全局抓取 ====================

    public void logAction(AnAction action, AnActionEvent event) {
        if (!capturing.get()) return;
        String actionClass = action.getClass().getSimpleName();
        String actionText = action.getTemplatePresentation().getText();
        String place = event.getPlace();
        log(Level.ACTION, "IDEA", actionClass,
                String.format("Text: %s | Place: %s", actionText, place));
    }

    public void logButtonClick(String componentClass, String text) {
        if (!capturing.get()) return;
        log(Level.BUTTON, "UI", componentClass, "Clicked: " + (text != null ? text : "unnamed"));
    }

    public void logFlowStart(String flowName, String... params) {
        if (!capturing.get()) return;
        StringBuilder sb = new StringBuilder("Flow started");
        if (params != null && params.length > 0) {
            // 对参数进行脱敏
            String maskedParams = String.join(", ", params);
            maskedParams = SensitiveDataMasker.maskText(maskedParams);
            sb.append(" | ").append(maskedParams);
        }
        log(Level.FLOW, "FLOW", flowName + "_START", sb.toString());
    }

    public void logFlowEnd(String flowName, boolean success, long durationMs) {
        if (!capturing.get()) return;
        log(Level.FLOW, "FLOW", flowName + "_END",
                String.format("Success: %b | Duration: %dms", success, durationMs));
    }

    public void logSshOperation(int serverId, String operation, String detail) {
        // 对 SSH 操作详情进行脱敏
        String maskedDetail = SensitiveDataMasker.maskText(detail);
        log(Level.SSH, "SSH", operation, String.format("Server: %d | %s", serverId, maskedDetail));
    }

    public void logFileTransfer(int serverId, String direction, String localPath, String remotePath, boolean success) {
        // 对文件路径进行脱敏
        String maskedLocal = SensitiveDataMasker.maskText(localPath);
        String maskedRemote = SensitiveDataMasker.maskText(remotePath);
        log(Level.FILE_TRANSFER, "TRANSFER", direction.toUpperCase(),
                String.format("Server: %d | Local: %s | Remote: %s | Success: %b",
                        serverId, maskedLocal, maskedRemote, success));
    }

    // ==================== 内部方法 ====================

    private void enqueueLog(Level level, String category, String action, String message, Throwable throwable) {
        if (!running) return;

        LogEntry entry = new LogEntry(level, category, action, message, throwable);
        if (!logQueue.offer(entry)) {
            IDE_LOG.warn("Log queue full, dropped: " + message);
        }
    }

    private void initLogFile() {
        writeLock.lock();
        try {
            cleanupOldLogs();
            currentLogFile = logDir.resolve(LOG_FILE_PREFIX + LOG_FILE_SUFFIX);
            if (Files.exists(currentLogFile)) {
                currentFileSize.set(Files.size(currentLogFile));
            } else {
                currentFileSize.set(0);
            }
        } catch (IOException e) {
            IDE_LOG.error("Failed to initialize log file", e);
        } finally {
            writeLock.unlock();
        }
    }

    private void startWriterThread() {
        writerExecutor.submit(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (entry != null) {
                        writeLogEntry(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    IDE_LOG.error("Error writing log", e);
                }
            }
        });
    }

    private void writeLogEntry(LogEntry entry) {
        writeLock.lock();
        try {
            checkAndRoll();

            if (currentLogFile == null) {
                initLogFile();
            }

            if (currentLogFile != null) {
                String logLine = formatLogLine(entry);
                try (BufferedWriter writer = Files.newBufferedWriter(currentLogFile,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    writer.write(logLine);
                    writer.newLine();
                    currentFileSize.addAndGet(logLine.length() + 1);
                } catch (IOException e) {
                    IDE_LOG.error("Failed to write log file", e);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private String formatLogLine(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entry.timestamp.format(TIME_FORMAT)).append("] ");

        // 高亮标记
        if (entry.level.isHighlight()) {
            sb.append("*** ");
        }

        sb.append("[").append(entry.level.getName()).append("] ");
        sb.append("[").append(entry.category).append("] ");
        sb.append("[").append(entry.action).append("] ");
        sb.append(entry.message);

        if (entry.throwable != null) {
            sb.append("\n  Exception: ").append(entry.throwable.getClass().getName())
              .append(": ").append(entry.throwable.getMessage());
            StackTraceElement[] stack = entry.throwable.getStackTrace();
            int depth = Math.min(stack.length, 8);
            for (int i = 0; i < depth; i++) {
                sb.append("\n    at ").append(stack[i]);
            }
            if (stack.length > depth) {
                sb.append("\n    ... ").append(stack.length - depth).append(" more");
            }
        }

        return sb.toString();
    }

    private void checkAndRoll() {
        if (currentFileSize.get() >= MAX_SINGLE_FILE_SIZE) {
            rollLog();
        }
    }

    private void rollLog() {
        try {
            if (currentLogFile == null || !Files.exists(currentLogFile)) return;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path rolledFile = logDir.resolve(LOG_FILE_PREFIX + "_" + timestamp + LOG_FILE_SUFFIX);
            Files.move(currentLogFile, rolledFile, StandardCopyOption.REPLACE_EXISTING);

            currentFileSize.set(0);
            currentLogFile = logDir.resolve(LOG_FILE_PREFIX + LOG_FILE_SUFFIX);

            cleanupOldLogs();
            IDE_LOG.info("Rolled log file to: " + rolledFile);
        } catch (IOException e) {
            IDE_LOG.error("Failed to roll log file", e);
        }
    }

    private void cleanupOldLogs() {
        try {
            File[] logFiles = logDir.toFile().listFiles((dir, name) ->
                    name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));

            if (logFiles == null || logFiles.length == 0) return;

            long totalSize = 0;
            for (File f : logFiles) {
                totalSize += f.length();
            }

            if (totalSize > MAX_LOG_SIZE) {
                java.util.Arrays.sort(logFiles, (a, b) ->
                        Long.compare(a.lastModified(), b.lastModified()));

                for (File f : logFiles) {
                    if (totalSize <= MAX_LOG_SIZE) break;
                    if (!f.getName().equals(LOG_FILE_PREFIX + LOG_FILE_SUFFIX)) {
                        long fileSize = f.length();
                        if (f.delete()) {
                            totalSize -= fileSize;
                            IDE_LOG.info("Deleted old log: " + f.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            IDE_LOG.error("Failed to cleanup old logs", e);
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "null";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...(truncated)";
    }

    public Path getLogFile() {
        return currentLogFile;
    }

    public Path getLogDir() {
        return logDir;
    }

    public String getStats() {
        return String.format("Enabled: %b | Capturing: %b | Queue: %d | File: %.2f MB",
                enabled.get(), capturing.get(), logQueue.size(),
                currentFileSize.get() / (1024.0 * 1024.0));
    }

    public void shutdown() {
        running = false;
        stopCapture();
        disable();
        try {
            writerExecutor.shutdown();
            if (!writerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            writerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        IDE_LOG.info("UnifiedLogger shutdown");
    }

    // ==================== 静态便捷方法 ====================

    public static void logToolCall(String toolName, String params) {
        getInstance().toolCall(toolName, params);
    }

    public static void logToolResult(String toolName, boolean success, String result, long durationMs) {
        getInstance().toolResult(toolName, success, result, durationMs);
    }

    public static void logDebug(String category, String message) {
        getInstance().debug(category, message);
    }

    public static void logInfo(String category, String message) {
        getInstance().info(category, message);
    }

    public static void logWarn(String category, String message) {
        getInstance().warn(category, message);
    }

    public static void logError(String category, String message) {
        getInstance().error(category, message);
    }

    public static void staticLogException(String category, String action, String message, Throwable throwable) {
        getInstance().logException(category, action, message, throwable);
    }

    public static void staticLogNull(String category, String fieldName, String context) {
        getInstance().logNullData(category, fieldName, context);
    }

    public static void staticLogButtonClick(String componentClass, String text) {
        getInstance().logButtonClick(componentClass, text);
    }

    public static void staticLogActionEvent(AnAction action, AnActionEvent event) {
        getInstance().logAction(action, event);
    }

    public static void staticLogFlow(String flowName, boolean start, boolean success, long durationMs) {
        if (start) {
            getInstance().logFlowStart(flowName);
        } else {
            getInstance().logFlowEnd(flowName, success, durationMs);
        }
    }
}