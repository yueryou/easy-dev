package io.github.yueryou.easydev.plugin.executor;

import io.github.yueryou.easydev.plugin.model.DelayCheckItem;
import io.github.yueryou.easydev.plugin.model.DelayCheckStep;
import io.github.yueryou.easydev.plugin.model.ExecutionContext;
import io.github.yueryou.easydev.plugin.model.PipelineStep;
import io.github.yueryou.easydev.plugin.model.StepResult;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.service.ISshService;
import tech.lin2j.idea.plugin.service.impl.SshjSshService;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.ssh.SshStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * 延迟检查步骤执行器
 *
 * 在设定的时间内周期性执行检测任务，所有检测项全部成功才认为本步骤成功。
 */
public class DelayCheckExecutor {

    /**
     * Shared HttpClient instance. HttpClient is immutable, thread-safe, and
     * connection-pooled, so a single instance should be reused across all calls.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private DelayCheckExecutor() {
        throw new IllegalStateException("Utility class");
    }

    public static StepResult execute(PipelineStep step, ExecutionContext context) {
        if (!(step instanceof DelayCheckStep)) {
            return StepResult.failure("Invalid step type: expected DelayCheckStep");
        }

        DelayCheckStep checkStep = (DelayCheckStep) step;
        int totalDuration = checkStep.getDuration();
        int interval = checkStep.getInterval();
        List<DelayCheckItem> items = checkStep.getCheckItems();

        context.getLogConsumer().accept("[延迟检查] 开始执行：" + checkStep.getName());
        context.getLogConsumer().accept("[延迟检查] 总时长: " + totalDuration + "s, 间隔: " + interval + "s, 检测项: " + items.size() + " 个");

        if (items.isEmpty()) {
            context.getLogConsumer().accept("[延迟检查] 未配置检测项，视为成功");
            return StepResult.success("未配置检测项", 0);
        }

        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(totalDuration);
        int round = 0;

        while (true) {
            Instant now = Instant.now();
            if (!now.isBefore(endTime)) {
                break;
            }
            round++;
            Instant roundStart = now;
            context.getLogConsumer().accept("");
            context.getLogConsumer().accept("[延迟检查] 第 " + round + " 轮检测开始（" + roundStart + "）");

            boolean allSuccess = true;
            for (DelayCheckItem item : items) {
                if (!Instant.now().isBefore(endTime)) {
                    context.getLogConsumer().accept("[延迟检查] 超时，停止检测");
                    break;
                }

                context.getLogConsumer().accept("  [检测] " + item.getType().getDisplayName() + ": " + item.getName());
                StepResult itemResult = executeCheckItem(item, context);

                if (itemResult.isSuccess()) {
                    context.getLogConsumer().accept("  [结果] 通过");
                } else {
                    allSuccess = false;
                    context.getLogConsumer().accept("  [结果] 失败 - " + itemResult.getErrorMessage());
                }
            }

            if (allSuccess) {
                Instant roundEnd = Instant.now();
                context.getLogConsumer().accept("");
                context.getLogConsumer().accept("[延迟检查] 所有检测项通过，第 " + round + " 轮完成（耗时 " + Duration.between(roundStart, roundEnd).getSeconds() + "s）");
                StepResult result = StepResult.success("延迟检查通过，共执行 " + round + " 轮", 0);
                result.setDuration(Duration.between(startTime, Instant.now()));
                return result;
            }

            Instant roundEnd = Instant.now();
            context.getLogConsumer().accept("[延迟检查] 第 " + round + " 轮完成（耗时 " + Duration.between(roundStart, roundEnd).getSeconds() + "s）");

            // 等待间隔时间
            if (Instant.now().isBefore(endTime)) {
                long remainingSecs = Duration.between(Instant.now(), endTime).getSeconds();
                long sleepTime = Math.min(interval, Math.max(1, remainingSecs));
                context.getLogConsumer().accept("[延迟检查] 等待 " + sleepTime + "s 后进行下一轮...");
                try {
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    context.getLogConsumer().accept("[延迟检查] 被中断");
                    return StepResult.failure("延迟检查被中断");
                }
            } else {
                break;
            }
        }

        context.getLogConsumer().accept("");
        context.getLogConsumer().accept("[延迟检查] 超时，检测未完成");
        StepResult result = StepResult.failure("延迟检查超时，检测未完成");
        result.setDuration(Duration.between(startTime, Instant.now()));
        return result;
    }

    /**
     * 执行单个检测项
     */
    private static StepResult executeCheckItem(DelayCheckItem item, ExecutionContext context) {
        switch (item.getType()) {
            case REMOTE_COMMAND:
                return executeRemoteCommand(item, context);
            case HTTP_REQUEST:
                return executeHttpRequest(item, context);
            default:
                return StepResult.failure("未知的检测类型: " + item.getType());
        }
    }

    /**
     * 执行远程命令检测
     */
    private static StepResult executeRemoteCommand(DelayCheckItem item, ExecutionContext context) {
        String serverId = item.getServerId();
        String command = item.getCommand();

        if (serverId == null || serverId.isEmpty()) {
            return StepResult.failure("未配置服务器");
        }
        if (command == null || command.isEmpty()) {
            return StepResult.failure("未配置命令");
        }

        try {
            SshServer server = ConfigHelper.getSshServerById(Integer.parseInt(serverId));
            if (server == null) {
                return StepResult.failure("服务器不存在: ID=" + serverId);
            }

            ISshService sshService = new SshjSshService();

            context.getLogConsumer().accept("    [命令] " + command);
            context.getLogConsumer().accept("    [服务器] " + server.getIp() + ":" + server.getPort());

            long start = System.currentTimeMillis();
            SshStatus status = sshService.execute(server, command);
            long duration = System.currentTimeMillis() - start;

            String output = status.getMessage();
            logMultiLineOutput(context.getLogConsumer(), "    [输出] ", output);
            context.getLogConsumer().accept("    [状态] " + (status.isSuccess() ? "成功" : "失败") + ", 耗时: " + duration + "ms");

            if (status.isSuccess()) {
                if (outputContainsExpected(output, item.getExpectedOutput())) {
                    return StepResult.success(output, 0);
                } else {
                    StepResult result = StepResult.failure("输出不包含期望字符串: " + item.getExpectedOutput());
                    result.setStdout(output);
                    result.setDuration(Duration.ofMillis(duration));
                    return result;
                }
            } else {
                StepResult result = StepResult.failure("远程命令执行失败: " + status.getMessage());
                result.setStdout(status.getMessage());
                result.setDuration(Duration.ofMillis(duration));
                return result;
            }
        } catch (NumberFormatException e) {
            return StepResult.failure("服务器 ID 格式错误: " + serverId);
        } catch (Exception e) {
            return StepResult.failure("远程命令执行异常: " + e.getMessage());
        }
    }

    /**
     * 执行 HTTP 请求检测
     */
    private static StepResult executeHttpRequest(DelayCheckItem item, ExecutionContext context) {
        String url = item.getUrl();
        if (url == null || url.isEmpty()) {
            return StepResult.failure("未配置 URL");
        }

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(item.getTimeout()))
                    .header("User-Agent", "Easy-Dev-Pipeline/1.0");

            String method = item.getHttpMethod() != null ? item.getHttpMethod().toUpperCase() : "GET";
            String httpBody = null;
            if ("POST".equals(method) && item.getHttpBody() != null && !item.getHttpBody().isEmpty()) {
                httpBody = item.getHttpBody();
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(httpBody, StandardCharsets.UTF_8));
                requestBuilder.header("Content-Type", "application/json");
            } else {
                requestBuilder.GET();
            }

            HttpRequest request = requestBuilder.build();

            context.getLogConsumer().accept("    [URL] " + url);
            context.getLogConsumer().accept("    [方法] " + method);
            if (httpBody != null) {
                context.getLogConsumer().accept("    [请求体] " + httpBody);
            }

            long start = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            int statusCode = response.statusCode();
            int expectedCode = item.getExpectedStatusCode() > 0 ? item.getExpectedStatusCode() : 200;
            String body = response.body();

            context.getLogConsumer().accept("    [状态码] " + statusCode + " (期望: " + expectedCode + ")");
            logMultiLineOutput(context.getLogConsumer(), "    [响应体] ", body);
            context.getLogConsumer().accept("    [耗时] " + duration + "ms");

            if (statusCode == expectedCode) {
                if (item.getExpectedOutput() == null || item.getExpectedOutput().isEmpty()) {
                    StepResult result = StepResult.success("HTTP " + statusCode + " - 未配置检测项，视为成功", 0);
                    result.setDuration(Duration.ofMillis(duration));
                    return result;
                }
                // Check expected output against the FULL response body before truncating
                if (!outputContainsExpected(body, item.getExpectedOutput())) {
                    String truncatedBody = body.length() > 500 ? body.substring(0, 500) + "..." : body;
                    StepResult result = StepResult.failure("响应内容不包含期望字符串: " + item.getExpectedOutput());
                    result.setStdout(truncatedBody);
                    result.setDuration(Duration.ofMillis(duration));
                    return result;
                }
                String displayBody = body.length() > 500 ? body.substring(0, 500) + "..." : body;
                StepResult result = StepResult.success("HTTP " + statusCode + " - " + displayBody, 0);
                result.setDuration(Duration.ofMillis(duration));
                return result;
            } else {
                StepResult result = StepResult.failure("HTTP 状态码不匹配: 期望 " + expectedCode + ", 实际 " + statusCode);
                result.setDuration(Duration.ofMillis(duration));
                return result;
            }
        } catch (Exception e) {
            return StepResult.failure("HTTP 请求异常: " + e.getMessage());
        }
    }

    // ==================== Utility Methods ====================

    /**
     * 检查输出是否包含期望字符串（不区分大小写）
     * @param output 命令输出
     * @param expected 期望包含的字符串，为空则返回 true
     * @return true 如果输出包含期望字符串，或期望字符串为空
     */
    private static boolean outputContainsExpected(String output, String expected) {
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        if (output == null) {
            return false;
        }
        return output.toLowerCase().contains(expected.toLowerCase());
    }

    /**
     * 格式化多行输出，每行添加指定前缀缩进
     */
    private static void logMultiLineOutput(Consumer<String> logConsumer, String prefix, String output) {
        if (output == null || output.isEmpty()) {
            logConsumer.accept(prefix + "(空)");
            return;
        }
        String[] lines = output.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                logConsumer.accept(prefix + lines[i]);
            } else {
                logConsumer.accept("    | " + lines[i]);
            }
        }
    }
}
