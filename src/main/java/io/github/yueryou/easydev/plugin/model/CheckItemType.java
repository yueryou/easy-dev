package io.github.yueryou.easydev.plugin.model;

/**
 * 延迟检查子项类型枚举
 */
public enum CheckItemType {
    REMOTE_COMMAND("远程命令"),
    HTTP_REQUEST("HTTP 请求");

    private final String displayName;

    CheckItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
