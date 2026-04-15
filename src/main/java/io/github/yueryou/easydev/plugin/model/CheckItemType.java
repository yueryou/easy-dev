package io.github.yueryou.easydev.plugin.model;

/**
 * 延迟检查子项类型枚举
 */
public enum CheckItemType {
    REMOTE_COMMAND("远程命令"),
    LOCAL_SCRIPT("本地脚本"),
    REMOTE_SCRIPT("远程脚本"),
    HTTP_REQUEST("HTTP 请求"),
    PORT_PROBE("端口探测");

    private final String displayName;

    CheckItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
