package io.github.yueryou.easydev.plugin.log;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类
 * 用于日志记录时对敏感字段进行脱敏处理，防止隐私泄露
 *
 * @author yuex
 * @date 2026/04/03
 */
public class SensitiveDataMasker {

    /**
     * 需要完全脱敏的字段名（密码、密钥等）
     */
    private static final Set<String> FULL_MASK_FIELDS = new HashSet<>();

    /**
     * 脱敏替换字符
     */
    private static final String MASK = "******";

    /**
     * 用户目录正则（用于替换字符串中间的路径）
     */
    private static final Pattern USER_HOME_PATTERN = Pattern.compile(
            "(/Users/[^/]+)|(/home/[^/]+)|(/root)|(C:/Users/[^/]+)|(D:/Users/[^/]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * IP 字段正则
     */
    private static final Pattern IP_FIELD_PATTERN = Pattern.compile(
            "(?i)\"(ip|host|hostname)\"\\s*:\\s*\"([^\"]+)\""
    );

    /**
     * 用户名字段正则
     */
    private static final Pattern USERNAME_FIELD_PATTERN = Pattern.compile(
            "(?i)\"(username|user|email)\"\\s*:\\s*\"([^\"]+)\""
    );

    /**
     * 路径字段正则
     */
    private static final Pattern PATH_FIELD_PATTERN = Pattern.compile(
            "(?i)\"(path|filePath|file|localPath|remotePath|location|dir|directory|pemPrivateKey)\"\\s*:\\s*\"([^\"]+)\""
    );

    static {
        // 完全脱敏字段：密码、密钥、token等
        FULL_MASK_FIELDS.add("password");
        FULL_MASK_FIELDS.add("passPhrase");
        FULL_MASK_FIELDS.add("passphrase");
        FULL_MASK_FIELDS.add("secret");
        FULL_MASK_FIELDS.add("token");
        FULL_MASK_FIELDS.add("apiKey");
        FULL_MASK_FIELDS.add("api_key");
        FULL_MASK_FIELDS.add("privateKey");
        FULL_MASK_FIELDS.add("private_key");
        FULL_MASK_FIELDS.add("pemPrivateKey");
        FULL_MASK_FIELDS.add("credential");
        FULL_MASK_FIELDS.add("auth");
    }

    /**
     * 对 JSON 字符串中的敏感字段进行脱敏
     *
     * @param json 原始 JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public static String maskJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String result = json;

        // 完全脱敏：匹配 "fieldName":"value" 或 "fieldName": "value"
        for (String field : FULL_MASK_FIELDS) {
            // 匹配字符串值
            result = result.replaceAll(
                    "(?i)\"(" + field + ")\"\\s*:\\s*\"[^\"]*\"",
                    "\"$1\":\"" + MASK + "\""
            );
            // 匹配数值或 null
            result = result.replaceAll(
                    "(?i)\"(" + field + ")\"\\s*:\\s*[^,}\\]\"]+",
                    "\"$1\":\"" + MASK + "\""
            );
        }

        // 部分脱敏 IP 地址
        result = maskByPattern(result, IP_FIELD_PATTERN, SensitiveDataMasker::maskIp);

        // 部分脱敏用户名
        result = maskByPattern(result, USERNAME_FIELD_PATTERN, SensitiveDataMasker::maskUsername);

        // 路径脱敏：替换用户目录
        result = maskPaths(result);

        return result;
    }

    /**
     * 使用正则模式进行脱敏
     */
    private static String maskByPattern(String text, Pattern pattern, MaskFunction maskFunc) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String value = matcher.group(2);
            String masked = maskFunc.apply(value);
            matcher.appendReplacement(sb, "\"" + fieldName + "\":\"" + masked + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @FunctionalInterface
    private interface MaskFunction {
        String apply(String value);
    }

    /**
     * 对普通字符串进行脱敏（检测并脱敏路径和敏感信息）
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public static String maskText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 脱敏用户目录路径
        return USER_HOME_PATTERN.matcher(text).replaceAll("~");
    }

    /**
     * IP 地址脱敏：保留前两段
     * 例如：192.168.1.100 -> 192.168.*.*
     */
    private static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        // IPv4
        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }

        // IPv6 或其他格式，只显示前几个字符
        if (ip.length() > 8) {
            return ip.substring(0, 4) + "****";
        }

        return "***";
    }

    /**
     * 用户名脱敏：保留首尾字符
     * 例如：admin -> a***n, root -> r**t
     */
    private static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return username;
        }

        int len = username.length();
        if (len <= 2) {
            return username.charAt(0) + "*";
        } else if (len <= 4) {
            return username.charAt(0) + "**" + username.charAt(len - 1);
        } else {
            return username.charAt(0) + "***" + username.charAt(len - 1);
        }
    }

    /**
     * 脱敏 JSON 中的路径字段
     */
    private static String maskPaths(String json) {
        Matcher matcher = PATH_FIELD_PATTERN.matcher(json);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String value = matcher.group(2);
            String masked = USER_HOME_PATTERN.matcher(value).replaceFirst("~");
            matcher.appendReplacement(sb, "\"" + fieldName + "\":\"" + masked + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 对 SshServer 关键信息进行脱敏
     *
     * @param ip       IP 地址
     * @param username 用户名
     * @return 脱敏后的标识字符串
     */
    public static String maskServerInfo(String ip, String username) {
        return "Server[" + maskIp(ip) + "/" + maskUsername(username) + "]";
    }

    /**
     * 对命令内容进行脱敏（可能包含密码等敏感信息）
     *
     * @param command 命令内容
     * @return 脱敏后的命令
     */
    public static String maskCommand(String command) {
        if (command == null || command.isEmpty()) {
            return command;
        }

        // 脱敏常见的密码参数
        String result = command;

        // -p password, --password=xxx
        result = result.replaceAll("(?i)(-p\\s+|--password[=\\s])\\S+", "$1" + MASK);

        // -Dpassword=xxx
        result = result.replaceAll("(?i)(-D\\w*password[=])\\S+", "$1" + MASK);

        // PASS=xxx, PASSWORD=xxx
        result = result.replaceAll("(?i)(\\b(PASS|PASSWORD|SECRET|TOKEN)\\s*[=:]\\s*)\\S+", "$1" + MASK);

        // 脱敏用户路径
        result = USER_HOME_PATTERN.matcher(result).replaceAll("~");

        return result;
    }

    /**
     * 判断字段名是否为敏感字段
     *
     * @param fieldName 字段名
     * @return 是否敏感
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lowerField = fieldName.toLowerCase();
        return FULL_MASK_FIELDS.stream().anyMatch(lowerField::equalsIgnoreCase);
    }
}