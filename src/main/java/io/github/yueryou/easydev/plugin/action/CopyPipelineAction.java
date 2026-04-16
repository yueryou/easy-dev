package io.github.yueryou.easydev.plugin.action;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.ui.TextTransferable;
import icons.MyIcons;
import org.jetbrains.annotations.NotNull;
import tech.lin2j.idea.plugin.action.NewUpdateThreadAction;
import io.github.yueryou.easydev.plugin.model.Pipeline;
import io.github.yueryou.easydev.plugin.ui.component.CommandPipelinePanel;

import java.util.function.Supplier;

/**
 * @author Easy Deploy Plugin
 * @date 2024/12/XX XX:XX
 */
public class CopyPipelineAction extends NewUpdateThreadAction {

    private final Supplier<Pipeline> provider;

    public CopyPipelineAction(Pipeline pipeline) {
        this(() -> pipeline);
    }

    public CopyPipelineAction(Supplier<Pipeline> provider) {
        super("Copy Pipeline", "Copy pipeline", MyIcons.Actions.Copy);
        this.provider = provider;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (provider.get() == null) {
            return;
        }
        Gson gson = new Gson();
        String json = gson.toJson(provider.get());
        CopyPasteManager.getInstance().setContents(new TextTransferable(json));

        // Auto-refresh the pipeline list after copying
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            // Refresh pipeline list by calling getAllPipelines which will update any cached lists
            io.github.yueryou.easydev.plugin.model.PipelineConfigPersistence.getAllPipelines();
        });
    }
}