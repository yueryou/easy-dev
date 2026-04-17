package tech.lin2j.idea.plugin.ui.settings;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tech.lin2j.idea.plugin.model.PluginSetting;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TemplateSettingsPanel.
 *
 * Note: Full UI testing requires IntelliJ platform runtime.
 * UI-dependent tests are marked with @Disabled and should be
 * run via ./gradlew test in an environment with the IDE loaded.
 */
public class TemplateSettingsPanelTest {

    // ============================================================
    // Logic-only tests (no IntelliJ runtime required)
    // ============================================================

    @Test
    public void pluginSetting_shouldHaveAutoSaveTemplateChangesDefaultTrue() {
        PluginSetting setting = new PluginSetting();
        assertTrue(setting.isAutoSaveTemplateChanges());
    }

    @Test
    public void pluginSetting_shouldHaveAutoValidateDefaultFalse() {
        PluginSetting setting = new PluginSetting();
        assertFalse(setting.isAutoValidateTemplateCredentials());
    }

    @Test
    public void pluginSetting_shouldToggleAutoSave() {
        PluginSetting setting = new PluginSetting();
        setting.setAutoSaveTemplateChanges(false);
        assertFalse(setting.isAutoSaveTemplateChanges());

        setting.setAutoSaveTemplateChanges(true);
        assertTrue(setting.isAutoSaveTemplateChanges());
    }

    @Test
    public void pluginSetting_shouldToggleAutoValidate() {
        PluginSetting setting = new PluginSetting();
        setting.setAutoValidateTemplateCredentials(true);
        assertTrue(setting.isAutoValidateTemplateCredentials());

        setting.setAutoValidateTemplateCredentials(false);
        assertFalse(setting.isAutoValidateTemplateCredentials());
    }

    // ============================================================
    // UI tests (require IntelliJ runtime)
    // ============================================================

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void panel_creation_shouldNotThrowException() {
        // This test requires the IntelliJ Platform to be loaded.
        // Run with: ./gradlew test --tests "TemplateSettingsPanelTest"
        // in an environment with the IDE available.
        fail("This test is disabled - remove @Disabled to run in IDE environment");
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void mainPanel_shouldHaveExpectedComponents() {
        fail("This test is disabled - remove @Disabled to run in IDE environment");
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void isModified_shouldReturnTrueWhenSettingsChanged() {
        fail("This test is disabled - remove @Disabled to run in IDE environment");
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void isModified_shouldReturnFalseWhenSettingsUnchanged() {
        fail("This test is disabled - remove @Disabled to run in IDE environment");
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void applyChanges_shouldSaveSettings() {
        fail("This test is disabled - remove @Disabled to run in IDE environment");
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void reset_shouldRestoreOriginalSettings() {
        fail("This test is disabled - remove @Disabled to run in IDE environment");
    }
}
