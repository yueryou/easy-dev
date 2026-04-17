package tech.lin2j.idea.plugin.model;

import tech.lin2j.idea.plugin.enums.AuthType;

import java.util.HashMap;
import java.util.Map;

public class CredentialTemplate implements UniqueModel {

    private String name;
    private String description;
    private String username;
    private Integer authType;
    private String encryptedData;
    private Map<String, String> customFields = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAuthType() {
        return authType;
    }

    public void setAuthType(Integer authType) {
        this.authType = authType;
    }

    public void setAuthType(AuthType authType) {
        if (authType != null) {
            this.authType = authType.getCode();
        }
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }

    @Override
    public String getUid() {
        // UniqueModel接口方法实现
        return null; // 将由ConfigPersistence.checkUid()方法填充
    }

    @Override
    public void setUid(String uid) {
        // 设置唯一标识符
        throw new UnsupportedOperationException("setUid should not be called directly");
    }
}