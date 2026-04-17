package tech.lin2j.idea.plugin.ui.settings;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.PluginSetting;
import tech.lin2j.idea.plugin.service.TemplateManager;
import tech.lin2j.idea.plugin.ui.dialog.TemplateManagementDialog;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Settings panel for credential template configuration.
 * Displays template statistics, toggle options, and a button
 * to open the template management dialog.
 *
 * @author lin2j
 * @date 2024/04/17
 */
public class TemplateSettingsPanel {

    private final Project project;
    private final PluginSetting setting;

    private JPanel mainPanel;
    private JBLabel statsLabel;
    private JBCheckBox autoSaveCheckBox;
    private JBCheckBox autoValidateCheckBox;
    private JButton manageButton;

    // Store initial values for isModified() check
    private boolean initialAutoSave;
    private boolean initialAutoValidate;

    public TemplateSettingsPanel(Project project) {
        this.project = project;
        this.setting = ConfigHelper.pluginSetting();
        initPanel();
    }

    /**
     * Initialize the main panel with all components.
     */
    private void initPanel() {
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(JBUI.Borders.empty(8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(4, 0);

        // Stats label
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        int templateCount = TemplateManager.getInstance().getAllTemplates().size();
        statsLabel = new JBLabel(MessagesBundle.getText("setting.template.stats", templateCount));
        mainPanel.add(statsLabel, gbc);

        // Auto-save checkbox
        gbc.gridy = 1;
        initialAutoSave = setting.isAutoSaveTemplateChanges();
        autoSaveCheckBox = new JBCheckBox(MessagesBundle.getText("setting.template.auto-save"));
        autoSaveCheckBox.setSelected(initialAutoSave);
        mainPanel.add(autoSaveCheckBox, gbc);

        // Auto-validate checkbox
        gbc.gridy = 2;
        initialAutoValidate = setting.isAutoValidateTemplateCredentials();
        autoValidateCheckBox = new JBCheckBox(MessagesBundle.getText("setting.template.auto-validate"));
        autoValidateCheckBox.setSelected(initialAutoValidate);
        mainPanel.add(autoValidateCheckBox, gbc);

        // Manage templates button
        gbc.gridy = 3;
        gbc.insets = JBUI.insetsTop(8);
        manageButton = new JButton(MessagesBundle.getText("setting.template.manage-button"));
        manageButton.addActionListener(e -> TemplateManagementDialog.show(project));
        mainPanel.add(manageButton, gbc);

        // Spacer
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        mainPanel.add(new JPanel(), gbc);
    }

    /**
     * Returns the main JPanel containing all template settings components.
     *
     * @return the main settings panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Checks whether the current settings differ from the saved settings.
     *
     * @return true if any setting has been modified
     */
    public boolean isModified() {
        return autoSaveCheckBox.isSelected() != setting.isAutoSaveTemplateChanges()
                || autoValidateCheckBox.isSelected() != setting.isAutoValidateTemplateCredentials();
    }

    /**
     * Applies the current UI settings to the persistent configuration.
     */
    public void applyChanges() {
        setting.setAutoSaveTemplateChanges(autoSaveCheckBox.isSelected());
        setting.setAutoValidateTemplateCredentials(autoValidateCheckBox.isSelected());
    }

    /**
     * Resets the UI to reflect the currently saved settings.
     */
    public void reset() {
        autoSaveCheckBox.setSelected(setting.isAutoSaveTemplateChanges());
        autoValidateCheckBox.setSelected(setting.isAutoValidateTemplateCredentials());

        // Refresh stats
        int templateCount = TemplateManager.getInstance().getAllTemplates().size();
        statsLabel.setText(MessagesBundle.getText("setting.template.stats", templateCount));
    }
}
