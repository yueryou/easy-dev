package tech.lin2j.idea.plugin.action.ftp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import io.github.yueryou.easydev.plugin.ui.SelectServersForUploadDialog;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import java.util.Arrays;
import java.util.List;

/**
 * Action to upload project files/folders to selected server(s)
 *
 * @author yueryou
 * @date 2026/04/21
 */
public class UploadToProjectFileAction extends AnAction {

    public UploadToProjectFileAction() {
        super(MessagesBundle.getText("action.project.uploadto.text"),
                MessagesBundle.getText("action.project.uploadto.description"),
                AllIcons.Actions.Upload);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null || files.length == 0) {
            return;
        }

        List<VirtualFile> selectedFiles = Arrays.asList(files);

        SelectServersForUploadDialog dialog = new SelectServersForUploadDialog(project, selectedFiles);
        dialog.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        e.getPresentation().setEnabledAndVisible(files != null && files.length > 0);
    }
}
