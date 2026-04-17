package tech.lin2j.idea.plugin.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.ssh.SshServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author linjinjia
 * @date 2022/4/24 17:52
 */
@State(
        name = "SimpleDeployConfig",
        storages = @Storage(value = "deploy-helper-settings.xml")
)
public class ConfigPersistence implements PersistentStateComponent<ConfigPersistence>, Serializable {

    private static final Logger LOG = Logger.getInstance(ConfigPersistence.class);

    private List<SshServer> sshServers;

    private List<Command> commands;

    private List<UploadProfile> uploadProfiles;

    private List<String> serverTags;

    private PluginSetting setting;

    private List<io.github.yueryou.easydev.plugin.model.Pipeline> pipelines;

    private List<CredentialTemplate> credentialTemplates;

    @Override
    public @Nullable ConfigPersistence getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ConfigPersistence state) {
        LOG.info("[ConfigPersistence] loadState() called");
        if (state != null && state.getPipelines() != null) {
            LOG.info("[ConfigPersistence] State has " + state.getPipelines().size() + " pipelines BEFORE copyBean");
            for (int i = 0; i < state.getPipelines().size(); i++) {
                io.github.yueryou.easydev.plugin.model.Pipeline p = state.getPipelines().get(i);
                LOG.info("[ConfigPersistence]   pipeline[" + i + "]: name=" + p.getName() +
                    ", uid=" + p.getUid() +
                    ", steps=" + (p.getSteps() != null ? p.getSteps().size() : "null"));
                if (p.getSteps() != null) {
                    for (int j = 0; j < p.getSteps().size(); j++) {
                        io.github.yueryou.easydev.plugin.model.PipelineStepWrapper w = p.getSteps().get(j);
                        LOG.info("[ConfigPersistence]     step[" + j + "]: type=" + (w != null ? w.getType() : "null") +
                            ", uid=" + (w != null ? w.getUid() : "null"));
                    }
                }
            }
        } else {
            LOG.info("[ConfigPersistence] State or pipelines is null");
        }
        XmlSerializerUtil.copyBean(state, this);
        if (this.getPipelines() != null) {
            LOG.info("[ConfigPersistence] This has " + this.getPipelines().size() + " pipelines AFTER copyBean");
            for (int i = 0; i < this.getPipelines().size(); i++) {
                io.github.yueryou.easydev.plugin.model.Pipeline p = this.getPipelines().get(i);
                LOG.info("[ConfigPersistence]   this.pipeline[" + i + "]: name=" + p.getName() +
                    ", uid=" + p.getUid() +
                    ", steps=" + (p.getSteps() != null ? p.getSteps().size() : "null"));
            }
        }
    }

    public List<io.github.yueryou.easydev.plugin.model.Pipeline> getPipelines() {
        if (pipelines == null) {
            pipelines = new CopyOnWriteArrayList<>();
        }
        // 过滤 null 元素（可能由于 XML 反序列化失败导致）
        pipelines.removeIf(Objects::isNull);
        checkUid(pipelines);
        return pipelines;
    }

    public void setPipelines(List<io.github.yueryou.easydev.plugin.model.Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public List<SshServer> getSshServers() {
        if (sshServers == null) {
            sshServers = new CopyOnWriteArrayList<>();
        }
        checkUid(sshServers);
        return sshServers;
    }

    public void setSshServers(List<SshServer> sshServers) {
        this.sshServers = sshServers;
    }

    public List<Command> getCommands() {
        if (commands == null) {
            commands = new CopyOnWriteArrayList<>();
        }
        checkUid(commands);
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public List<UploadProfile> getUploadProfiles() {
        if (uploadProfiles == null) {
            uploadProfiles = new CopyOnWriteArrayList<>();
        }
        //
        int maxProfileId = uploadProfiles.stream().map(UploadProfile::getId)
                .filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0);
        for (UploadProfile profile : uploadProfiles) {
            if (profile.getId() == null) {
                profile.setId(++maxProfileId);
            }
        }
        checkUid(uploadProfiles);
        return uploadProfiles;
    }

    public void setUploadProfiles(List<UploadProfile> uploadProfiles) {
        this.uploadProfiles = uploadProfiles;
    }

    public List<String> getServerTags() {
        if (serverTags == null) {
            serverTags = new ArrayList<>();
            serverTags.add("Default");
        }
        return serverTags;
    }

    public void setServerTags(List<String> serverTags) {
        this.serverTags = serverTags;
    }

    public PluginSetting getSetting() {
        if (setting == null) {
            setting = new PluginSetting();
        }
        return setting;
    }

    public void setSetting(PluginSetting setting) {
        this.setting = setting;
    }

    public List<CredentialTemplate> getCredentialTemplates() {
        if (credentialTemplates == null) {
            credentialTemplates = new CopyOnWriteArrayList<>();
        }
        checkUid(credentialTemplates);
        return credentialTemplates;
    }

    public void setCredentialTemplates(List<CredentialTemplate> credentialTemplates) {
        this.credentialTemplates = credentialTemplates;
    }

    public void addCredentialTemplate(CredentialTemplate template) {
        if (template != null) {
            getCredentialTemplates().add(template);
        }
    }

    public boolean removeCredentialTemplate(String templateId) {
        return getCredentialTemplates().removeIf(t -> t.getUid().equals(templateId));
    }

    public CredentialTemplate findTemplateById(String templateId) {
        return getCredentialTemplates().stream()
            .filter(t -> t.getUid().equals(templateId))
            .findFirst()
            .orElse(null);
    }

    private void checkUid(List<? extends UniqueModel> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.stream()
            .filter(d -> d != null)  // 跳过 null 元素
            .forEach(d -> {
                if (d.getUid() == null) {
                    d.setUid(UUID.randomUUID().toString());
                }
            });
    }
}
