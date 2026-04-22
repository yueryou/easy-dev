package tech.lin2j.idea.plugin.model;

import tech.lin2j.idea.plugin.enums.AuthType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a reusable credential template that can be associated with SSH servers.
 * Templates encapsulate authentication information (username, auth type, encrypted
 * credentials) so that multiple servers can share common credentials without
 * duplicating sensitive data.
 *
 * <p>UIDs are auto-generated via {@link #ensureUid()} when templates are persisted
 * through {@code ConfigPersistence.checkUid()}, so callers should not rely on
 * {@link #getUid()} to produce a value before persistence.
 *
 * @author linjinjia
 * @see tech.lin2j.idea.plugin.service.TemplateManager
 */
public class CredentialTemplate implements UniqueModel {

    private String uid;

    private String name;
    private String description;
    private String username;
    private Integer authType;
    private String encryptedData;
    private Map<String, String> customFields = new HashMap<>();

    public CredentialTemplate() {
    }

    /**
     * Copy constructor that creates a deep copy of the given template.
     *
     * @param other the template to copy from
     */
    public CredentialTemplate(CredentialTemplate other) {
        this.uid = other.uid;
        this.name = other.name;
        this.description = other.description;
        this.username = other.username;
        this.authType = other.authType;
        this.encryptedData = other.encryptedData;
        this.customFields = other.customFields != null ? new HashMap<>(other.customFields) : new HashMap<>();
    }

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
        return uid;
    }

    /**
     * Ensures that this template has a non-null, non-empty UID.
     * If the UID is currently {@code null} or empty, a random UUID
     * is generated and assigned.
     *
     * <p>Call this method before persisting the template if you need
     * a guaranteed UID. In practice, {@code ConfigPersistence.checkUid()}
     * handles this automatically during serialization.
     */
    public void ensureUid() {
        if (uid == null || uid.isEmpty()) {
            uid = UUID.randomUUID().toString();
        }
    }

    @Override
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return name;
    }
}
