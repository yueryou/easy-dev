package tech.lin2j.idea.plugin.action;

import com.intellij.openapi.project.Project;
import io.github.yueryou.easydev.plugin.ui.dialog.TaskPanelDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TaskPanelDialogAction implements ActionListener {

    private final int sshId;
    private final Project project;

    public TaskPanelDialogAction(int sshId, Project project) {
        this.sshId = sshId;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TaskPanelDialog dialog = new TaskPanelDialog(project, sshId);
        dialog.show();
    }
}
