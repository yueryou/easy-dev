package tech.lin2j.idea.plugin.ui.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Configurable implementation that integrates SSH credential template
 * settings with IntelliJ's Settings/Preferences dialog.
 *
 * @author lin2j
 * @date 2024/04/17
 */
public class TemplateConfigurable implements Configurable {

    private TemplateSettingsPanel settingsPanel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "SSH 密钥模板";
    }

    @Override
    public @Nullable JComponent createComponent() {
        Project project = ProjectManager.getInstance().getDefaultProject();
        settingsPanel = new TemplateSettingsPanel(project);
        return settingsPanel.getMainPanel();
    }

    @Override
    public boolean isModified() {
        return settingsPanel != null && settingsPanel.isModified();
    }

    @Override
    public void apply() {
        if (settingsPanel != null) {
            settingsPanel.applyChanges();
        }
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}
