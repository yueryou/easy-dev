package io.github.yueryou.easydev.plugin.model;

/**
 * 延迟检查子项
 */
public class DelayCheckItem {

    private CheckItemType type;
    private String name;

    // 远程命令
    private String serverId;
    private String command;

    // 脚本（本地/远程）
    private String scriptPath;

    // HTTP 请求
    private String url;
    private String httpMethod = "GET";
    private String httpBody;
    private int expectedStatusCode = 200;

    // 端口探测
    private String host;
    private int port;

    // 通用
    private int timeout = 10;

    /**
     * 期望输出包含的字符串（不区分大小写），为空时不检查输出内容
     */
    private String expectedOutput;

    public DelayCheckItem() {
    }

    public DelayCheckItem(CheckItemType type, String name) {
        this.type = type;
        this.name = name;
    }

    public CheckItemType getType() {
        return type;
    }

    public void setType(CheckItemType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getHttpBody() {
        return httpBody;
    }

    public void setHttpBody(String httpBody) {
        this.httpBody = httpBody;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    /**
     * 获取简短描述，用于列表展示
     */
    public String getBriefDescription() {
        switch (type) {
            case REMOTE_COMMAND:
                return command != null && !command.isEmpty() ? command.substring(0, Math.min(30, command.length())) : "(空命令)";
            case LOCAL_SCRIPT:
            case REMOTE_SCRIPT:
                return scriptPath != null ? scriptPath : "(未选择脚本)";
            case HTTP_REQUEST:
                return url != null ? httpMethod + " " + url : "(未配置 URL)";
            case PORT_PROBE:
                return host + ":" + port;
            default:
                return "(未知类型)";
        }
    }
}
