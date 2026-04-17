package tech.lin2j.idea.plugin.ui.dialog;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.enums.AuthType;
import tech.lin2j.idea.plugin.model.CredentialTemplate;
import tech.lin2j.idea.plugin.service.TemplateAuditLogger;
import tech.lin2j.idea.plugin.service.TemplateManager;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dialog for creating or editing credential templates.
 *
 * @author lin2j
 * @date 2024/04/17
 */
public class CredentialTemplateEditDialog extends DialogWrapper {

    private final Project project;
    private final CredentialTemplate templateToEdit;
    private final boolean isNewMode;

    private CredentialTemplate resultTemplate;

    private JTextField nameField;
    private JTextField descriptionField;
    private JTextField usernameField;
    private JRadioButton passwordAuthRadio;
    private JRadioButton keyAuthRadio;
    private JPasswordField passwordField;
    private TextFieldWithBrowseButton keyPathField;
    private JPanel customFieldsPanel;
    private JButton addCustomFieldButton;
    private final java.util.List<JComponent[]> customFieldRows = new java.util.ArrayList<>();

    public CredentialTemplateEditDialog(@Nullable Project project, @Nullable CredentialTemplate templateToEdit) {
        super(project);
        this.project = project;
        this.templateToEdit = templateToEdit;
        this.isNewMode = (templateToEdit == null);

        setTitle(isNewMode
                ? MessagesBundle.getText("dialog.credential-template.new.title")
                : MessagesBundle.getText("dialog.credential-template.edit.title"));
        setSize(500, 480);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel root = new JPanel(new GridLayoutManager(10, 2, new Insets(10, 10, 10, 10), -1, 5));

        int row = 0;

        // Name
        root.add(new JBLabel(MessagesBundle.getText("dialog.credential-template.name")),
                new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        nameField = new JBTextField();
        if (!isNewMode && templateToEdit != null) {
            nameField.setText(templateToEdit.getName());
        }
        root.add(nameField,
                new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(300, -1), null));
        row++;

        // Description
        root.add(new JBLabel(MessagesBundle.getText("dialog.credential-template.description")),
                new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        descriptionField = new JBTextField();
        if (!isNewMode && templateToEdit != null) {
            descriptionField.setText(templateToEdit.getDescription());
        }
        root.add(descriptionField,
                new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(300, -1), null));
        row++;

        // Username
        root.add(new JBLabel(MessagesBundle.getText("dialog.credential-template.username")),
                new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        usernameField = new JBTextField();
        if (!isNewMode && templateToEdit != null) {
            usernameField.setText(templateToEdit.getUsername());
        }
        root.add(usernameField,
                new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(300, -1), null));
        row++;

        // Auth type
        root.add(new JBLabel(MessagesBundle.getText("dialog.credential-template.auth-type")),
                new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup authGroup = new ButtonGroup();
        passwordAuthRadio = new JRadioButton(MessagesBundle.getText("dialog.panel.host.basic.auth-type.password"));
        keyAuthRadio = new JRadioButton(MessagesBundle.getText("dialog.panel.host.basic.auth-type.private"));
        authGroup.add(passwordAuthRadio);
        authGroup.add(keyAuthRadio);
        authPanel.add(passwordAuthRadio);
        authPanel.add(keyAuthRadio);

        // Default to password auth
        passwordAuthRadio.setSelected(true);
        if (!isNewMode && templateToEdit != null) {
            if (AuthType.PEM_PRIVATE_KEY.getCode().equals(templateToEdit.getAuthType())) {
                keyAuthRadio.setSelected(true);
            } else {
                passwordAuthRadio.setSelected(true);
            }
        }

        root.add(authPanel,
                new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        row++;

        // Password field
        root.add(new JBLabel(MessagesBundle.getText("dialog.credential-template.password")),
                new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        passwordField = new JPasswordField();
        root.add(passwordField,
                new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(300, -1), null));
        row++;

        // Key path field
        root.add(new JBLabel(MessagesBundle.getText("dialog.credential-template.key-path")),
                new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        keyPathField = new TextFieldWithBrowseButton();
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        keyPathField.addBrowseFolderListener(
                new TextBrowseFolderListener(descriptor, project));
        if (!isNewMode && templateToEdit != null && AuthType.PEM_PRIVATE_KEY.getCode().equals(templateToEdit.getAuthType())) {
            keyPathField.setText(templateToEdit.getEncryptedData());
        }
        root.add(keyPathField,
                new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(300, -1), null));
        row++;

        // Custom fields section
        JPanel customSection = new JPanel(new BorderLayout(5, 5));
        customSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                MessagesBundle.getText("dialog.credential-template.custom-fields"),
                TitledBorder.LEFT, TitledBorder.TOP));
        customFieldsPanel = new JPanel(new GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), 5, 3));
        addCustomFieldButton = new JButton(MessagesBundle.getText("dialog.credential-template.custom-fields.add"));
        addCustomFieldButton.addActionListener(e -> addCustomFieldRow());
        customSection.add(customFieldsPanel, BorderLayout.CENTER);
        customSection.add(addCustomFieldButton, BorderLayout.SOUTH);

        // Populate existing custom fields
        if (!isNewMode && templateToEdit != null && templateToEdit.getCustomFields() != null) {
            for (Map.Entry<String, String> entry : templateToEdit.getCustomFields().entrySet()) {
                addCustomFieldRow(entry.getKey(), entry.getValue());
            }
        }

        root.add(customSection,
                new GridConstraints(row, 0, 1, 2, GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));

        // Auth type listener to toggle fields
        passwordAuthRadio.addActionListener(e -> updateFieldVisibility());
        keyAuthRadio.addActionListener(e -> updateFieldVisibility());
        updateFieldVisibility();

        return root;
    }

    private void updateFieldVisibility() {
        boolean isPassword = passwordAuthRadio.isSelected();
        passwordField.setEnabled(isPassword);
        keyPathField.setEnabled(!isPassword);
    }

    private void addCustomFieldRow() {
        addCustomFieldRow("", "");
    }

    private void addCustomFieldRow(String key, String value) {
        int index = customFieldRows.size();
        JTextField keyField = new JBTextField(key);
        JTextField valueField = new JBTextField(value);
        keyField.setColumns(10);
        valueField.setColumns(10);

        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rowPanel.add(new JLabel(MessagesBundle.getText("dialog.credential-template.custom-fields.key") + ":"));
        rowPanel.add(keyField);
        rowPanel.add(new JLabel(MessagesBundle.getText("dialog.credential-template.custom-fields.value") + ":"));
        rowPanel.add(valueField);

        JButton removeButton = new JButton("X");
        removeButton.setPreferredSize(new Dimension(30, 25));
        removeButton.addActionListener(e -> {
            customFieldsPanel.remove(rowPanel);
            customFieldRows.removeIf(r -> r[0] == rowPanel);
            customFieldsPanel.revalidate();
            customFieldsPanel.repaint();
        });
        rowPanel.add(removeButton);

        customFieldRows.add(new JComponent[]{rowPanel, keyField, valueField});

        GridConstraints gc = new GridConstraints(index, 0, 1, 2,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null);
        customFieldsPanel.add(rowPanel, gc);
        customFieldsPanel.revalidate();
        customFieldsPanel.repaint();
    }

    @Override
    protected void doOKAction() {
        if (!validateInput()) {
            return;
        }

        CredentialTemplate template;
        if (isNewMode) {
            template = new CredentialTemplate();
            template.setUid(UUID.randomUUID().toString());
        } else {
            template = templateToEdit;
        }

        template.setName(nameField.getText().trim());
        template.setDescription(descriptionField.getText().trim());
        template.setUsername(usernameField.getText().trim());
        template.setAuthType(passwordAuthRadio.isSelected() ? AuthType.PASSWORD : AuthType.PEM_PRIVATE_KEY);

        if (passwordAuthRadio.isSelected()) {
            String password = new String(passwordField.getPassword());
            if (!password.isEmpty()) {
                TemplateManager.saveTemplatePassword(template.getUid(), password);
            }
            template.setEncryptedData(null);
        } else {
            template.setEncryptedData(keyPathField.getText().trim());
        }

        // Collect custom fields
        Map<String, String> customFields = new LinkedHashMap<>();
        for (JComponent[] row : customFieldRows) {
            JTextField keyField = (JTextField) row[1];
            JTextField valueField = (JTextField) row[2];
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();
            if (!key.isEmpty()) {
                customFields.put(key, value);
            }
        }
        template.setCustomFields(customFields);

        if (isNewMode) {
            TemplateManager.getInstance().addTemplate(template);
            TemplateAuditLogger.logTemplateCreation(template.getUid(), template.getName());
        } else {
            TemplateAuditLogger.logTemplateModification(template.getUid(), template.getName());
        }

        resultTemplate = template;
        super.doOKAction();
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Messages.showErrorDialog(
                    MessagesBundle.getText("dialog.credential-template.validation.name-required"),
                    MessagesBundle.getText("dialog.credential-template.validation.title"));
            nameField.requestFocusInWindow();
            return false;
        }

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            Messages.showErrorDialog(
                    MessagesBundle.getText("dialog.credential-template.validation.username-required"),
                    MessagesBundle.getText("dialog.credential-template.validation.title"));
            usernameField.requestFocusInWindow();
            return false;
        }

        if (passwordAuthRadio.isSelected()) {
            String password = new String(passwordField.getPassword());
            if (isNewMode && password.isEmpty()) {
                Messages.showErrorDialog(
                        MessagesBundle.getText("dialog.credential-template.validation.password-required"),
                        MessagesBundle.getText("dialog.credential-template.validation.title"));
                passwordField.requestFocusInWindow();
                return false;
            }
        } else {
            String keyPath = keyPathField.getText().trim();
            if (isNewMode && keyPath.isEmpty()) {
                Messages.showErrorDialog(
                        MessagesBundle.getText("dialog.credential-template.validation.key-required"),
                        MessagesBundle.getText("dialog.credential-template.validation.title"));
                keyPathField.requestFocusInWindow();
                return false;
            }
        }

        return true;
    }

    @Nullable
    public CredentialTemplate getTemplate() {
        return resultTemplate;
    }
}
