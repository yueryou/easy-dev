package tech.lin2j.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import icons.MyIcons;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.ui.dialog.HostSettingsDialog;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import java.util.function.Supplier;

public class CreateNewConnectionFromServerAction extends NewUpdateThreadAction {

    private final Project project;
    private final Supplier<SshServer> provider;

    public CreateNewConnectionFromServerAction(Project project, SshServer server) {
        this(project, () -> server);
    }

    public CreateNewConnectionFromServerAction(Project project, Supplier<SshServer> provider) {
        super(MessagesBundle.getText("action.new-connection.prompt.title"),
              MessagesBundle.getText("action.new-connection.prompt.description"),
              MyIcons.Actions.AddHost);
        this.project = project;
        this.provider = provider;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        createNewConnection();
    }

    /**
     * Creates a new connection by cloning the source server configuration.
     * This method can be called directly without going through the action system.
     */
    public void createNewConnection() {
        SshServer source = provider.get();
        if (source == null) return;

        SshServer clone = source.clone();
        clone.setId(null);
        clone.setUid(null);

        new HostSettingsDialog(project, clone).show();
    }
}
