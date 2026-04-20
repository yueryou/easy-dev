package tech.lin2j.idea.plugin.ui.settings;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TemplateConfigurable.
 *
 * Note: Full UI testing requires IntelliJ platform runtime.
 * UI-dependent tests are marked with @Disabled and should be
 * run via ./gradlew test in an environment with the IDE loaded.
 */
public class TemplateConfigurableTest {

    // ============================================================
    // Logic-only tests (no IntelliJ runtime required)
    // ============================================================

    @Test
    public void configurable_shouldBeInstantiable() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        assertNotNull(configurable);
    }

    @Test
    public void getDisplayName_shouldReturnCorrectText() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        assertEquals("OneKey 配置", configurable.getDisplayName());
    }

    // ============================================================
    // UI tests (require IntelliJ runtime)
    // ============================================================

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void createComponent_shouldReturnNonNullPanel() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        assertNotNull(configurable.createComponent());
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void isModified_shouldReturnFalseInitially() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        configurable.createComponent();
        assertFalse(configurable.isModified());
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void reset_shouldNotThrowException() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        configurable.createComponent();
        configurable.reset();
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void apply_shouldNotThrowException() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        configurable.createComponent();
        configurable.apply();
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void disposeUIResources_shouldNotThrowException() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        configurable.createComponent();
        configurable.disposeUIResources();
    }

    @Test
    @Disabled("Requires IntelliJ Platform runtime for UI component creation")
    public void configurable_shouldDelegateToSettingsPanel() {
        TemplateConfigurable configurable = new TemplateConfigurable();
        configurable.createComponent();
        // Verify that isModified delegates to the panel
        assertFalse(configurable.isModified());
    }
}
