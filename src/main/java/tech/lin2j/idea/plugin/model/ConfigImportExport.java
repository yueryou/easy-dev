package tech.lin2j.idea.plugin.model;

import tech.lin2j.idea.plugin.ssh.SshServer;

import java.util.List;
import io.github.yueryou.easydev.plugin.model.Pipeline;

/**
 * @author linjinjia
 * @date 2024/7/17 20:52
 */
public class ConfigImportExport {

    private ExportOptions options;

    private String version;

    private List<String> serverTags;

    private List<HostInfo> hostInfos;

    private List<Pipeline> pipelines;

    private List<CredentialTemplate> credentialTemplates;

    public ExportOptions getOptions() {
        return options;
    }

    public void setOptions(ExportOptions options) {
        this.options = options;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getServerTags() {
        return serverTags;
    }

    public void setServerTags(List<String> serverTags) {
        this.serverTags = serverTags;
    }

    public List<HostInfo> getHostInfos() {
        return hostInfos;
    }

    public void setHostInfos(List<HostInfo> hostInfos) {
        this.hostInfos = hostInfos;
    }

    public List<Pipeline> getPipelines() {
        return pipelines;
    }

    public void setPipelines(List<Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public List<CredentialTemplate> getCredentialTemplates() {
        return credentialTemplates;
    }

    public void setCredentialTemplates(List<CredentialTemplate> credentialTemplates) {
        this.credentialTemplates = credentialTemplates;
    }

    public static class HostInfo {
        private SshServer server;
        private List<Command> commands;
        private List<UploadProfile> uploadProfiles;

        public SshServer getServer() {
            return server;
        }

        public void setServer(SshServer server) {
            this.server = server;
        }

        public List<Command> getCommands() {
            return commands;
        }

        public void setCommands(List<Command> commands) {
            this.commands = commands;
        }

        public List<UploadProfile> getUploadProfiles() {
            return uploadProfiles;
        }

        public void setUploadProfiles(List<UploadProfile> uploadProfiles) {
            this.uploadProfiles = uploadProfiles;
        }

    }
}