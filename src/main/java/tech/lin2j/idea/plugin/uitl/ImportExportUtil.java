package tech.lin2j.idea.plugin.uitl;

import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import tech.lin2j.idea.plugin.model.Command;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.ConfigImportExport;
import tech.lin2j.idea.plugin.model.CredentialTemplate;
import tech.lin2j.idea.plugin.model.ExportOptions;
import tech.lin2j.idea.plugin.model.UploadProfile;
import tech.lin2j.idea.plugin.ssh.SshServer;
import io.github.yueryou.easydev.plugin.model.Pipeline;
import io.github.yueryou.easydev.plugin.model.PipelineConfigPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author linjinjia
 * @date 2024/7/17 20:54
 */
public class ImportExportUtil {

    private ImportExportUtil() {
    }

    /**
     * Exports configuration based on the provided options.
     *
     * @param options The options specifying which fields to export.
     * @return A {@link ConfigImportExport} object containing the exported configuration data.
     */
    public static ConfigImportExport exportBaseOnOptions(ExportOptions options) throws Exception {
        ConfigImportExport dto = new ConfigImportExport();
        dto.setOptions(options);
        dto.setVersion(EasyDeployPluginUtil.version());
        if (options.isServerTags()) {
            dto.setServerTags(ConfigHelper.getServerTags());
        }

        List<SshServer> sshServers = ConfigHelper.sshServers();
        if (CollectionUtils.isEmpty(sshServers)) {
            return dto;
        }

        List<ConfigImportExport.HostInfo> hostInfos = new ArrayList<>();
        for (SshServer server : sshServers) {
            ConfigImportExport.HostInfo hostInfo = new ConfigImportExport.HostInfo();
            hostInfo.setServer(server.clone());

            // Commands and upload profiles are now global, not per-server
            // Export all commands and profiles
            if (options.isCommand()) {
                List<Command> cloneCommands = new ArrayList<>();
                for (Command command : ConfigHelper.getAllCommands()) {
                    cloneCommands.add(new Command(command));
                }
                hostInfo.setCommands(cloneCommands);
            }

            if (options.isUploadProfile()) {
                List<UploadProfile> cloneProfiles = new ArrayList<>();
                for (UploadProfile uploadProfile : ConfigHelper.getAllUploadProfiles()) {
                    cloneProfiles.add(uploadProfile.clone());
                }
                hostInfo.setUploadProfiles(cloneProfiles);
            }

            hostInfos.add(hostInfo);
        }
        dto.setHostInfos(hostInfos);

        // pipeline
        if (options.isPipeline()) {
            List<Pipeline> clonePipelines = new ArrayList<>();
            for (Pipeline pipeline : ConfigHelper.getAllPipelines()) {
                clonePipelines.add(new Pipeline(pipeline));
            }
            dto.setPipelines(clonePipelines);
        }

        // credential template
        if (options.isCredentialTemplate()) {
            List<CredentialTemplate> cloneTemplates = new ArrayList<>();
            for (CredentialTemplate template : ConfigHelper.getCredentialTemplates()) {
                cloneTemplates.add(new CredentialTemplate(template));
            }
            dto.setCredentialTemplates(cloneTemplates);
        }

        return dto;
    }

    /**
     * Imports the provided configuration.
     *
     * @param newConfig The new configuration to be imported.
     * @return A {@link ConfigImportExport} object containing the old configuration data.
     * @throws Exception If an error occurs during the import process.
     */
    public static ConfigImportExport importConfig(ConfigImportExport newConfig) throws Exception {
        ConfigImportExport origin = exportBaseOnOptions(allExport());
        ExportOptions options = newConfig.getOptions();
        // server tag
        if (options.isServerTags()) {
            List<String> serverTags = ConfigHelper.getServerTags();
            List<String> newServerTags = newConfig.getServerTags();
            for (String nst : newServerTags) {
                if (!serverTags.contains(nst)) {
                    serverTags.add(nst);
                }
            }
        }

        // server
        Map<Integer, Integer> commandIdMap = new HashMap<>();
        Map<Integer, Integer> sshIdMap = new HashMap<>();
        for (ConfigImportExport.HostInfo hostInfo : newConfig.getHostInfos()) {
            SshServer newSever = hostInfo.getServer();
            int oldSshId = newSever.getId();
            int newSshId = ConfigHelper.maxSshServerId() + 1;
            newSever.setId(newSshId);
            ConfigHelper.addSshServer(newSever);
            sshIdMap.put(oldSshId, newSshId);
            // command
            // Commands are now global, import without sshId binding
            if (options.isCommand() && CollectionUtils.isNotEmpty(hostInfo.getCommands())) {
                hostInfo.getCommands().forEach(newCmd -> {
                    int oldCmdId = newCmd.getId();
                    int newCmdId = ConfigHelper.maxCommandId() + 1;
                    newCmd.setId(newCmdId);
                    newCmd.setSshId(null); // Clear sshId for new global format
                    ConfigHelper.addCommand(newCmd);

                    commandIdMap.put(oldCmdId, newCmdId);
                });
            }
            // upload profile
            // Upload profiles are now global, import without sshId binding
            if (options.isUploadProfile() && CollectionUtils.isNotEmpty(hostInfo.getUploadProfiles())) {
                hostInfo.getUploadProfiles().forEach(newProfile -> {
                    newProfile.setId(ConfigHelper.maxUploadProfileId() + 1);
                    newProfile.setSshId(null); // Clear sshId for new global format
                    // Map all command IDs (commandId, preCommandId, postCommandId)
                    if (newProfile.getCommandId() != null) {
                        newProfile.setCommandId(commandIdMap.get(newProfile.getCommandId()));
                    }
                    if (newProfile.getPreCommandId() != null) {
                        newProfile.setPreCommandId(commandIdMap.get(newProfile.getPreCommandId()));
                    }
                    if (newProfile.getPostCommandId() != null) {
                        newProfile.setPostCommandId(commandIdMap.get(newProfile.getPostCommandId()));
                    }
                    ConfigHelper.addUploadProfile(newProfile);
                });
            }
        }

        // proxy
        sshIdMap.forEach((oldId, newId) -> {
            SshServer newSshServer = ConfigHelper.getSshServerById(newId);
            if (newSshServer.getProxy() != null) {
                int oldProxyId = newSshServer.getProxy();
                newSshServer.setProxy(sshIdMap.get(oldProxyId));
            }
        });

        // pipeline
        if (options.isPipeline() && CollectionUtils.isNotEmpty(newConfig.getPipelines())) {
            for (Pipeline newPipeline : newConfig.getPipelines()) {
                // Clear existing ID to force creation of new pipeline
                newPipeline.setId(null);
                if (newPipeline.getUid() != null) {
                    // Check if a pipeline with this UID already exists, skip duplicate
                    Pipeline existing = ConfigHelper.getPipelineByUid(newPipeline.getUid());
                    if (existing != null) {
                        continue;
                    }
                }
                PipelineConfigPersistence.addPipeline(newPipeline);
            }
        }

        // credential template
        Map<String, String> templateIdMap = new HashMap<>();
        if (options.isCredentialTemplate() && CollectionUtils.isNotEmpty(newConfig.getCredentialTemplates())) {
            for (CredentialTemplate newTemplate : newConfig.getCredentialTemplates()) {
                String oldUid = newTemplate.getUid();
                // Check if a template with this UID already exists, skip duplicate
                CredentialTemplate existing = ConfigHelper.findTemplateById(newTemplate.getUid());
                if (existing != null) {
                    templateIdMap.put(oldUid, existing.getUid());
                    continue;
                }
                // Ensure UID is set, add template
                if (newTemplate.getUid() == null) {
                    newTemplate.setUid(UUID.randomUUID().toString());
                }
                ConfigHelper.addCredentialTemplate(newTemplate);
                templateIdMap.put(oldUid, newTemplate.getUid());
            }
        }

        // Remap server templateId references for newly imported servers
        if (options.isCredentialTemplate() && !templateIdMap.isEmpty()) {
            sshIdMap.values().forEach(newSshId -> {
                SshServer server = ConfigHelper.getSshServerById(newSshId);
                if (server != null && server.getTemplateId() != null) {
                    String newTemplateId = templateIdMap.get(server.getTemplateId());
                    if (newTemplateId != null) {
                        server.setTemplateId(newTemplateId);
                    }
                }
            });
        }

        return origin;
    }

    public static ExportOptions allExport() {
        ExportOptions options = new ExportOptions();
        options.setServerTags(true);
        options.setCommand(true);
        options.setUploadProfile(true);
        options.setPipeline(true);
        options.setCredentialTemplate(true);
        return options;
    }

    public static void exportConfigToJsonFile(ConfigImportExport data,
                                              String filepath,
                                              String password) throws IOException {
        String content = new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(data);
        EncryptionUtil.encryptContentIntoFile(content, filepath, password);
    }
}