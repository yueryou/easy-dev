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
import java.util.Objects;
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
        if (CollectionUtils.isNotEmpty(sshServers)) {
            List<ConfigImportExport.HostInfo> hostInfos = new ArrayList<>();
            for (SshServer server : sshServers) {
                ConfigImportExport.HostInfo hostInfo = new ConfigImportExport.HostInfo();
                hostInfo.setServer(server.clone());
                hostInfo.setExportedPassword(server.getPassword());
                hostInfo.setExportedPassPhrase(server.getPassPhrase());
                hostInfos.add(hostInfo);
            }
            dto.setHostInfos(hostInfos);
        }

        if (options.isCommand()) {
            List<Command> cloneCommands = new ArrayList<>();
            for (Command command : ConfigHelper.getAllCommands()) {
                cloneCommands.add(new Command(command));
            }
            dto.setCommands(cloneCommands);
        }

        if (options.isUploadProfile()) {
            List<UploadProfile> cloneProfiles = new ArrayList<>();
            for (UploadProfile uploadProfile : ConfigHelper.getAllUploadProfiles()) {
                cloneProfiles.add(uploadProfile.clone());
            }
            dto.setUploadProfiles(cloneProfiles);
        }

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

        // server - import independently
        Map<Integer, Integer> sshIdMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(newConfig.getHostInfos())) {
            for (ConfigImportExport.HostInfo hostInfo : newConfig.getHostInfos()) {
                SshServer newServer = hostInfo.getServer();
                int oldSshId = newServer.getId();

                // Deduplicate by IP+port+username
                SshServer existing = findExistingServer(newServer);
                if (existing != null) {
                    sshIdMap.put(oldSshId, existing.getId());
                    restoreServerPassword(existing, hostInfo.getExportedPassword(), hostInfo.getExportedPassPhrase());
                    continue;
                }

                int newSshId = ConfigHelper.maxSshServerId() + 1;
                newServer.setId(newSshId);
                ConfigHelper.addSshServer(newServer);
                sshIdMap.put(oldSshId, newSshId);
                restoreServerPassword(newServer, hostInfo.getExportedPassword(), hostInfo.getExportedPassPhrase());
            }

            // proxy
            sshIdMap.forEach((oldId, newId) -> {
                SshServer sshServer = ConfigHelper.getSshServerById(newId);
                if (sshServer.getProxy() != null) {
                    int oldProxyId = sshServer.getProxy();
                    sshServer.setProxy(sshIdMap.get(oldProxyId));
                }
            });
        }

        // command - import independently from servers
        if (options.isCommand() && CollectionUtils.isNotEmpty(newConfig.getCommands())) {
            newConfig.getCommands().forEach(newCmd -> {
                // Skip if UID already exists
                if (newCmd.getUid() != null && !newCmd.getUid().isEmpty()) {
                    Command existingCmd = findCommandByUid(newCmd.getUid());
                    if (existingCmd != null) {
                        return;
                    }
                }
                newCmd.setId(ConfigHelper.maxCommandId() + 1);
                newCmd.setSshId(null);
                ConfigHelper.addCommand(newCmd);
            });
        }

        // upload profile - import independently from servers
        if (options.isUploadProfile() && CollectionUtils.isNotEmpty(newConfig.getUploadProfiles())) {
            newConfig.getUploadProfiles().forEach(newProfile -> {
                // Skip if UID already exists
                if (newProfile.getUid() != null && !newProfile.getUid().isEmpty()) {
                    UploadProfile existingProfile = findUploadProfileByUid(newProfile.getUid());
                    if (existingProfile != null) {
                        return;
                    }
                }
                newProfile.setId(ConfigHelper.maxUploadProfileId() + 1);
                newProfile.setSshId(null);
                ConfigHelper.addUploadProfile(newProfile);
            });
        }

        // pipeline
        if (options.isPipeline() && CollectionUtils.isNotEmpty(newConfig.getPipelines())) {
            for (Pipeline newPipeline : newConfig.getPipelines()) {
                newPipeline.setId(null);
                if (newPipeline.getUid() != null) {
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
                if (newTemplate.getUid() == null || newTemplate.getUid().isEmpty()) {
                    newTemplate.setUid(UUID.randomUUID().toString());
                }
                String oldUid = newTemplate.getUid();
                CredentialTemplate existing = ConfigHelper.findTemplateById(oldUid);
                if (existing != null) {
                    templateIdMap.put(oldUid, existing.getUid());
                    continue;
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

    private static SshServer findExistingServer(SshServer server) {
        for (SshServer existing : ConfigHelper.sshServers()) {
            if (Objects.equals(existing.getIp(), server.getIp())
                    && Objects.equals(existing.getPort(), server.getPort())
                    && Objects.equals(existing.getUsername(), server.getUsername())) {
                return existing;
            }
        }
        return null;
    }

    private static void restoreServerPassword(SshServer server, String password, String passPhrase) {
        if (password != null && !password.isEmpty()) {
            server.setPassword(password);
        }
        if (passPhrase != null && !passPhrase.isEmpty()) {
            server.setPassPhrase(passPhrase);
        }
    }

    private static Command findCommandByUid(String uid) {
        for (Command cmd : ConfigHelper.getAllCommands()) {
            if (uid.equals(cmd.getUid())) {
                return cmd;
            }
        }
        return null;
    }

    private static UploadProfile findUploadProfileByUid(String uid) {
        for (UploadProfile profile : ConfigHelper.getAllUploadProfiles()) {
            if (uid.equals(profile.getUid())) {
                return profile;
            }
        }
        return null;
    }
}