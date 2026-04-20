package tech.lin2j.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import icons.MyIcons;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.event.ApplicationContext;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.event.TableRefreshEvent;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import java.util.UUID;
import java.util.function.Supplier;

public class CopySshServerAction extends NewUpdateThreadAction {

    private final Project project;
    private final Supplier<SshServer> provider;

    public CopySshServerAction(Project project, SshServer server) {
        this(project, () -> server);
    }

    public CopySshServerAction(Project project, Supplier<SshServer> provider) {
        super(MessagesBundle.getText("action.copy-server.prompt.title"),
              MessagesBundle.getText("action.copy-server.prompt.description"),
              MyIcons.Actions.Copy);
        this.project = project;
        this.provider = provider;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        SshServer source = provider.get();
        if (source == null) return;

        String newIp = Messages.showInputDialog(
            project,
            MessagesBundle.getText("action.copy-server.prompt.message"),
            MessagesBundle.getText("action.copy-server.prompt.title"),
            Messages.getQuestionIcon()
        );

        if (StringUtil.isEmpty(newIp)) return;

        SshServer copy = source.clone();
        copy.setId(ConfigHelper.maxSshServerId() + 1);
        copy.setUid(UUID.randomUUID().toString());

        // Support IP:port format
        if (newIp.contains(":")) {
            String[] parts = newIp.split(":");
            copy.setIp(parts[0].trim());
            try {
                copy.setPort(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ex) {
                copy.setPort(source.getPort());
            }
        } else {
            copy.setIp(newIp.trim());
        }

        ConfigHelper.addSshServer(copy);
        ApplicationContext.getApplicationContext().publishEvent(new TableRefreshEvent());
    }
}
