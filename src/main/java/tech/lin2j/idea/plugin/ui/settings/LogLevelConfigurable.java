package tech.lin2j.idea.plugin.ui.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.PluginSetting;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Objects;

/**
 * Log Level configuration for Easy Deploy plugin
 * Allows users to configure the logging level for the UnifiedLogger
 *
 * @author yuex
 * @date 2026/04/16
 */
public class LogLevelConfigurable implements SearchableConfigurable {

    private final PluginSetting setting = ConfigHelper.pluginSetting();

    private ComboBox<String> logLevelCombo;
    private JCheckBox enableUnifiedLoggerCheck;

    private static final String[] LOG_LEVELS = {"DEBUG", "INFO", "WARN", "ERROR", "OFF"};

    @Override
    public @NotNull @NonNls String getId() {
        return "ED-LogLevel";
    }

    @Override
    public String getDisplayName() {
        return MessagesBundle.getText("setting.log.level");
    }

    @Override
    public @Nullable JComponent createComponent() {
        return createLogLevelPanel();
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(logLevelCombo.getItemAt(logLevelCombo.getSelectedIndex()), setting.getUnifiedLogLevel());
    }

    @Override
    public void apply() {
        String selectedLevel = logLevelCombo.getItemAt(logLevelCombo.getSelectedIndex());
        String previousLevel = setting.getUnifiedLogLevel();
        setting.setUnifiedLogLevel(selectedLevel);

        // Notify logger of level change if it's different
        if (!Objects.equals(previousLevel, selectedLevel)) {
            io.github.yueryou.easydev.plugin.log.UnifiedLogger.getInstance().updateLogLevel(selectedLevel);
        }
    }

    @Override
    public void reset() {
        logLevelCombo.setSelectedItem(setting.getUnifiedLogLevel());
    }

    private JComponent createLogLevelPanel() {
        // Create log level combo box
        logLevelCombo = new ComboBox<>();
        logLevelCombo.setModel(new CollectionComboBoxModel<>(Arrays.asList(LOG_LEVELS)));
        logLevelCombo.setSelectedItem(setting.getUnifiedLogLevel());

        // Create description label
        JLabel descriptionLabel = new JLabel(MessagesBundle.getText("setting.log.level.description"));
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(11f));

        // Create the form
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(MessagesBundle.getText("setting.log.level.combo"), logLevelCombo)
                .addComponent(descriptionLabel)
                .getPanel();

        // Wrap in a border layout for proper alignment
        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);

        return result;
    }
}