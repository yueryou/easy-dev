package io.github.yueryou.easydev.plugin.action;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.github.yueryou.easydev.plugin.log.UnifiedLogger;
import icons.MyIcons;
import io.github.yueryou.easydev.plugin.model.Pipeline;
import io.github.yueryou.easydev.plugin.model.PipelineConfigPersistence;
import org.jetbrains.annotations.NotNull;
import tech.lin2j.idea.plugin.action.NewUpdateThreadAction;
import tech.lin2j.idea.plugin.event.ApplicationContext;
import tech.lin2j.idea.plugin.model.event.PipelineRefreshEvent;

import java.util.UUID;

import java.awt.datatransfer.DataFlavor;

/**
 * @author Easy Deploy Plugin
 * @date 2024/12/XX XX:XX
 */
public class PastePipelineAction extends NewUpdateThreadAction {

    private final Project project;
    private final UnifiedLogger logger = UnifiedLogger.getInstance();

    public PastePipelineAction(Project project) {
        super("Paste Pipeline", "Paste pipeline", MyIcons.Actions.Paste);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            // Get clipboard content
            String json = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);

            if (json == null || json.trim().isEmpty()) {
                return;
            }

            // Parse JSON to Pipeline object
            Gson gson = new Gson();
            Pipeline pipeline = gson.fromJson(json, Pipeline.class);

            if (pipeline == null) {
                return;
            }

            // Generate new UID and clear ID for the pasted pipeline
            if (pipeline.getUid() == null) {
                pipeline.setUid(java.util.UUID.randomUUID().toString());
            }
            pipeline.setId(null); // Clear ID to ensure new record is created

            // Prompt user for pipeline name
            String originalName = pipeline.getName();
            String defaultName = "Copy of " + originalName;
            String newName = Messages.showInputDialog(project, "Enter name for the copied pipeline:", "Paste Pipeline", Messages.getQuestionIcon(), defaultName, null);

            if (newName == null || newName.trim().isEmpty()) {
                return; // User cancelled
            }

            pipeline.setName(newName);

            // Add the new pipeline to persistence
            PipelineConfigPersistence.addPipeline(pipeline);

            // Auto-refresh the pipeline list after pasting and show success message
            ApplicationManager.getApplication().invokeLater(() -> {
                logger.debug("PastePipelineAction", "Publishing PipelineRefreshEvent after successful paste");

                // Show success message
                Messages.showInfoMessage(project,
                    String.format("Pipeline '%s' has been successfully created.", newName),
                    "Pipeline Created");

                // Publish refresh event to notify UI components to update
                ApplicationContext.getApplicationContext().publishEvent(new PipelineRefreshEvent());
                logger.debug("PastePipelineAction", "PipelineRefreshEvent published successfully");
            });

        } catch (Exception ex) {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(project,
                    "Failed to paste pipeline: " + ex.getMessage(),
                    "Paste Error");
            });
        }
    }
}