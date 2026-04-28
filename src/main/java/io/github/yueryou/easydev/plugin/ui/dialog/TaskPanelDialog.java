package io.github.yueryou.easydev.plugin.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.Nullable;
import io.github.yueryou.easydev.plugin.ui.component.CommandManagePanel;
import io.github.yueryou.easydev.plugin.ui.component.CommandPipelinePanel;
import tech.lin2j.idea.plugin.event.ApplicationContext;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.*;
import java.awt.*;

public class TaskPanelDialog extends DialogWrapper {
    private final Project project;
    private final int sshId;
    private final JPanel root = new JPanel(new BorderLayout());
    private final CommandManagePanel commandManagePanel;
    private final CommandPipelinePanel commandPipelinePanel;
    private JBTabbedPane tabs;

    public TaskPanelDialog(@Nullable Project project, int sshId) {
        super(project);
        this.project = project;
        this.sshId = sshId;
        // Pass sshId so panels execute on the specified server
        commandManagePanel = new CommandManagePanel(project, sshId);
        commandPipelinePanel = new CommandPipelinePanel(project, sshId);

        // Double-click callback: close dialog
        commandManagePanel.setOnDoubleClickExecute(() -> {
            super.doOKAction();
        });
        commandPipelinePanel.setOnDoubleClickExecute(() -> {
            super.doOKAction();
        });

        setTitle(MessagesBundle.getText("dialog.task.title"));
        setSize(500, 0);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        String manageTab = MessagesBundle.getText("dialog.command.tab.manage");
        String pipelineTab = MessagesBundle.getText("dialog.command.tab.task");

        tabs = new JBTabbedPane();
        tabs.addTab(manageTab, commandManagePanel.createUI());
        tabs.addTab(pipelineTab, commandPipelinePanel.createUI());

        root.add(tabs);
        ApplicationContext.getApplicationContext().addApplicationListener(commandManagePanel);
        ApplicationContext.getApplicationContext().addApplicationListener(commandPipelinePanel);
        return tabs;
    }
}
