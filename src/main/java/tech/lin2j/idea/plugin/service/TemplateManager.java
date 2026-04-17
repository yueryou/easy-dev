package tech.lin2j.idea.plugin.service;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import tech.lin2j.idea.plugin.model.CredentialTemplate;
import tech.lin2j.idea.plugin.model.ConfigPersistence;
import tech.lin2j.idea.plugin.ssh.SshServer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TemplateManager {

    private static final Logger LOG = Logger.getInstance(TemplateManager.class);
    private static TemplateManager instance;
    private final ConfigPersistence configPersistence;

    public static synchronized TemplateManager getInstance() {
        if (instance == null) {
            instance = new TemplateManager();
        }
        return instance;
    }

    private TemplateManager() {
        this.configPersistence = ApplicationManager.getApplication().getService(ConfigPersistence.class);
        if (configPersistence == null) {
            LOG.error("Failed to get ConfigPersistence service");
        }
    }

    // Create template from existing SSH server
    public static CredentialTemplate createTemplateFromSsh(SshServer server, String templateName) {
        CredentialTemplate template = new CredentialTemplate();
        template.setName(templateName);
        template.setUsername(server.getUsername());
        template.setAuthType(server.getAuthType());
        template.setUid(UUID.randomUUID().toString());

        // Store password in IntelliJ credential store
        saveTemplatePassword(template.getUid(), server.getPassword());

        return template;
    }

    // Get template password securely
    public static String getTemplatePassword(String templateId) {
        return loadPassword("TEMPLATE_" + templateId);
    }

    // Save template password to credential store
    public static void saveTemplatePassword(String templateId, String password) {
        if (password != null && !password.isEmpty()) {
            savePassword(password, "TEMPLATE_" + templateId);
        }
    }

    // Load password from credential store
    private static String loadPassword(String key) {
        try {
            CredentialAttributes attributes = new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("Easy Dev Templates", key)
            );
            PasswordSafe passwordSafe = PasswordSafe.getInstance();

            Credentials credentials = passwordSafe.get(attributes);
            if (credentials != null) {
                return credentials.getPasswordAsString();
            }
            return passwordSafe.getPassword(attributes);
        } catch (Exception e) {
            LOG.error("Failed to load password for key: " + key, e);
            return null;
        }
    }

    private static void savePassword(String password, String key) {
        try {
            CredentialAttributes attributes = new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("Easy Dev Templates", key)
            );
            Credentials credentials = new Credentials(key, password);
            PasswordSafe.getInstance().set(attributes, credentials);
        } catch (Exception e) {
            LOG.error("Failed to save password for key: " + key, e);
        }
    }

    // Template CRUD operations
    public List<CredentialTemplate> getAllTemplates() {
        return configPersistence.getCredentialTemplates();
    }

    public void addTemplate(CredentialTemplate template) {
        configPersistence.addCredentialTemplate(template);
    }

    public void removeTemplate(String templateId) {
        configPersistence.removeCredentialTemplate(templateId);
    }

    public CredentialTemplate getTemplate(String templateId) {
        return configPersistence.getCredentialTemplates().stream()
            .filter(t -> t.getUid().equals(templateId))
            .findFirst()
            .orElse(null);
    }
}