package tech.lin2j.idea.plugin.ssh;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.util.xmlb.annotations.Transient;
import tech.lin2j.idea.plugin.enums.AuthType;
import tech.lin2j.idea.plugin.model.UniqueModel;
import tech.lin2j.idea.plugin.service.TemplateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author linjinjia
 * @date 2022/4/25 15:06
 */
public class SshServer implements Cloneable, UniqueModel {

    public static final SshServer None = new SshServer();

    private Integer id;

    private String uid;

    private String ip;

    private List<String> ips;

    private Integer port;

    private String username;

    /**
     * @deprecated Passwords are now managed via the IntelliJ credential store
     *     and template system. This field is retained only for backward-compatible
     *     deserialization and should not be accessed directly.
     */
    @Deprecated
    @Transient
    private String password;

    private String tag;

    private String description;

    private Integer proxy;

    /**
     * authenticate with password or other ways
     *
     * @see AuthType
     */
    private Integer authType;

    /**
     * file path of ssh private key，
     * the default value is "~/.ssh/id_rsa"
     */
    private String pemPrivateKey;

    /** UID of the credential template used to populate credentials */
    private String templateId;

    /** Flag indicating whether template credentials are enabled for this server */
    private boolean useTemplateCredentials;

    /**
     * the command exit code that indicates the command is executed successfully
     */
    private Integer successCommandExitCode;

    public SshServer(String ip) {
        this.ip = ip;
    }

    public SshServer() {
    }

    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("Easy Dev", key)
        );
    }

    @Transient
    public String getPassword() {
        if (useTemplateCredentials && templateId != null && !templateId.isEmpty()) {
            return TemplateManager.getTemplatePassword(templateId);
        }
        return loadPassword(getKey());
    }

    @Transient
    public void setPassword(String password) {
        if (useTemplateCredentials && templateId != null && !templateId.isEmpty()) {
            TemplateManager.saveTemplatePassword(templateId, password);
        } else {
            savePassword(password, getKey());
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public List<String> getIpList() {
        if (ips != null && !ips.isEmpty()) {
            return ips;
        }
        if (ip != null && !ip.isEmpty()) {
            return Arrays.asList(ip.split(","));
        }
        return new ArrayList<>();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return ip;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAuthType() {
        return authType;
    }

    public void setAuthType(Integer authType) {
        this.authType = authType;
    }

    public String getPemPrivateKey() {
        return pemPrivateKey;
    }

    public void setPemPrivateKey(String pemPrivateKey) {
        this.pemPrivateKey = pemPrivateKey;
    }

    /**
     * Returns the UID of the credential template associated with this server.
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Sets the UID of the credential template to use for this server.
     */
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /**
     * Returns whether template credentials are enabled for this server.
     */
    public boolean isUseTemplateCredentials() {
        return useTemplateCredentials;
    }

    /**
     * Sets whether template credentials should be used for this server.
     */
    public void setUseTemplateCredentials(boolean useTemplateCredentials) {
        this.useTemplateCredentials = useTemplateCredentials;
    }

    @Transient
    public String getPassPhrase() {
        return loadPassword(getPassPhraseKey());
    }

    @Transient
    public void setPassPhrase(String passPhrase) {
        savePassword(passPhrase, getPassPhraseKey());
    }

    public Integer getProxy() {
        return proxy;
    }

    public void setProxy(Integer proxy) {
        this.proxy = proxy;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public void setUid(String uuid) {
        this.uid = uuid;
    }

    @Override
    public SshServer clone() {
        try {
            SshServer clone = (SshServer) super.clone();
            // Deep copy mutable state so the clone cannot affect the original
            if (this.ips != null) {
                clone.ips = new ArrayList<>(this.ips);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getSuccessCommandExitCode() {
        return Objects.requireNonNullElse(successCommandExitCode, 0);
    }

    public void setSuccessCommandExitCode(Integer successCommandExitCode) {
        this.successCommandExitCode = successCommandExitCode;
    }

    private String getKey() {
        return "SD$" + username + "@" + ip + ":" + port;
    }

    private String getPassPhraseKey() {
        return "SD$PASSPHRASE" + username + "@" + ip + ":" + port;
    }

    private String loadPassword(String key) {
        CredentialAttributes attributes = createCredentialAttributes(key);
        PasswordSafe passwordSafe = PasswordSafe.getInstance();

        Credentials credentials = passwordSafe.get(attributes);
        if (credentials != null) {
            return credentials.getPasswordAsString();
        }
        return passwordSafe.getPassword(attributes);
    }

    private void savePassword(String password, String key) {
        CredentialAttributes attributes = createCredentialAttributes(key);
        Credentials credentials = new Credentials(key, password);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    /**
     * Check if the command execution is successful based on the exit code.
     * Some servers may return a non-zero exit code after a successful command execution.
     *
     * @param exitCode the exit code of the command
     * @return true if the command execution is successful, false otherwise
     */
    public boolean isCommandSuccess(int exitCode) {
        return getSuccessCommandExitCode().equals(exitCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshServer sshServer = (SshServer) o;
        return Objects.equals(id, sshServer.id)
                && Objects.equals(ip, sshServer.ip)
                && Objects.equals(port, sshServer.port)
                && Objects.equals(username, sshServer.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ip, port, username);
    }
}