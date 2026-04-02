package io.github.yueryou.easydev.plugin.executor;

import io.github.yueryou.easydev.plugin.model.ExecutionContext;
import io.github.yueryou.easydev.plugin.model.LocalCommandStep;
import io.github.yueryou.easydev.plugin.model.PipelineStep;
import io.github.yueryou.easydev.plugin.model.StepResult;
import tech.lin2j.idea.plugin.model.Command;
import tech.lin2j.idea.plugin.model.ConfigHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地命令执行器 - 支持实时日志输出
 */
public class LocalCommandExecutor {

    private static final String LOG_PREFIX = "[LocalCommand] ";
    private static final long OUTPUT_WAIT_TIMEOUT_MS = 5000L;
    private static final int INITIAL_BUFFER_CAPACITY = 4096;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "LocalCommand-OutputGobbler");
        t.setDaemon(true);
        return t;
    });

    private LocalCommandExecutor() {
        throw new IllegalStateException("Utility class");
    }

    public static StepResult execute(PipelineStep step, ExecutionContext context) {
        if (!(step instanceof LocalCommandStep)) {
            return StepResult.failure("Invalid step type: expected LocalCommandStep");
        }

        LocalCommandStep localStep = (LocalCommandStep) step;
        context.getLogConsumer().accept(LOG_PREFIX + "开始执行：" + localStep.getName());

        // 解析变量和路径（支持相对路径）
        String command = context.resolve(localStep.getCommand());
        String workingDir = localStep.getWorkingDir() != null && !localStep.getWorkingDir().isEmpty()
            ? context.resolvePath(localStep.getWorkingDir())
            : null;
        int timeout = localStep.getTimeout() > 0 ? localStep.getTimeout() : 300;

        // 如果配置了 commandId，使用已有命令
        if (localStep.getCommandId() != null && !localStep.getCommandId().isEmpty()) {
            Command cmd = ConfigHelper.getCommandById(Integer.parseInt(localStep.getCommandId()));
            if (cmd != null) {
                command = context.resolve(cmd.generateCmdLine(workingDir));
                context.getLogConsumer().accept(LOG_PREFIX + "使用预定义命令：" + cmd.getTitle());
            } else {
                context.getLogConsumer().accept(LOG_PREFIX + "警告：未找到 commandId=" + localStep.getCommandId() + " 的 command");
            }
        }

        context.getLogConsumer().accept(LOG_PREFIX + "执行命令：" + command);

        long startTime = System.currentTimeMillis();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (IS_WINDOWS) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }

            if (workingDir != null && !workingDir.trim().isEmpty()) {
                processBuilder.directory(new java.io.File(workingDir));
            }

            Process process = processBuilder.start();

            StringBuilder stdoutBuilder = new StringBuilder(INITIAL_BUFFER_CAPACITY);
            StringBuilder stderrBuilder = new StringBuilder(INITIAL_BUFFER_CAPACITY);
            AtomicBoolean outputDone = new AtomicBoolean(false);
            AtomicBoolean errorDone = new AtomicBoolean(false);

            // 异步消费标准输出
            EXECUTOR.submit(() -> consumeStream(process.getInputStream(), stdoutBuilder, outputDone, context));

            // 异步消费错误输出
            EXECUTOR.submit(() -> consumeStream(process.getErrorStream(), stderrBuilder, errorDone, context));

            // 等待完成，支持超时
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                StepResult result = StepResult.failure("命令执行超时（" + timeout + "秒）");
                result.setDuration(java.time.Duration.ofMillis(System.currentTimeMillis() - startTime));
                return result;
            }

            // 等待输出流消费完成
            long waitStart = System.currentTimeMillis();
            while ((!outputDone.get() || !errorDone.get())
                    && (System.currentTimeMillis() - waitStart < OUTPUT_WAIT_TIMEOUT_MS)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
                context.getLogConsumer().accept(LOG_PREFIX + "执行成功，退出码：" + exitCode);
                StepResult result = StepResult.success(stdoutBuilder.toString().trim(), exitCode);
                result.setDuration(java.time.Duration.ofMillis(duration));
                return result;
            } else {
                context.getLogConsumer().accept(LOG_PREFIX + "执行失败，退出码：" + exitCode);
                StepResult result = StepResult.failure("命令执行失败，退出码：" + exitCode);
                result.setExitCode(exitCode);
                result.setStdout(stdoutBuilder.toString().trim());
                result.setStderr(stderrBuilder.toString().trim());
                result.setDuration(java.time.Duration.ofMillis(duration));
                return result;
            }

        } catch (IOException e) {
            context.getLogConsumer().accept(LOG_PREFIX + "执行异常：" + e.getMessage());
            StepResult result = StepResult.failure("IO 异常：" + e.getMessage());
            result.setDuration(java.time.Duration.ofMillis(System.currentTimeMillis() - startTime));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.getLogConsumer().accept(LOG_PREFIX + "执行中断");
            StepResult result = StepResult.failure("命令执行被中断");
            result.setDuration(java.time.Duration.ofMillis(System.currentTimeMillis() - startTime));
            return result;
        }
    }

    private static void consumeStream(java.io.InputStream stream, StringBuilder builder,
                                      AtomicBoolean done, ExecutionContext context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                context.getLogConsumer().accept(LOG_PREFIX + line);
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            context.getLogConsumer().accept(LOG_PREFIX + "读取输出流异常：" + e.getMessage());
        } finally {
            done.set(true);
        }
    }
}
