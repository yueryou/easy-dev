package tech.lin2j.idea.plugin.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.enums.AuthType;
import tech.lin2j.idea.plugin.model.CredentialTemplate;
import tech.lin2j.idea.plugin.service.TemplateAuditLogger;
import tech.lin2j.idea.plugin.service.TemplateManager;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for managing credential templates.
 * Provides a table view with create, edit, and delete operations.
 *
 * @author lin2j
 * @date 2024/04/17
 */
public class TemplateManagementDialog extends DialogWrapper {

    private final Project project;
    private final TemplateTableModel tableModel;
    private JBTable templateTable;
    private SearchTextField searchField;
    private List<CredentialTemplate> allTemplates;

    public TemplateManagementDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle(MessagesBundle.getText("dialog.template-management.title"));
        setSize(700, 450);

        allTemplates = new ArrayList<>(TemplateManager.getInstance().getAllTemplates());
        tableModel = new TemplateTableModel(new ArrayList<>(allTemplates));
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(JBUI.Borders.empty(8));

        // Toolbar panel
        JPanel toolbar = new JPanel(new BorderLayout(5, 0));

        // Search field
        searchField = new SearchTextField();
        searchField.setTextAndAddToHistory(MessagesBundle.getText("dialog.template-management.search.placeholder"));
        searchField.addTextListener(() -> filterTable());
        toolbar.add(searchField, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton newButton = new JButton(MessagesBundle.getText("dialog.template-management.btn.new"));
        newButton.addActionListener(e -> onCreateTemplate());
        JButton editButton = new JButton(MessagesBundle.getText("dialog.template-management.btn.edit"));
        editButton.addActionListener(e -> onEditTemplate());
        JButton deleteButton = new JButton(MessagesBundle.getText("dialog.template-management.btn.delete"));
        deleteButton.addActionListener(e -> onDeleteTemplate());

        buttonPanel.add(newButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        toolbar.add(buttonPanel, BorderLayout.SOUTH);

        root.add(toolbar, BorderLayout.NORTH);

        // Table
        templateTable = new JBTable(tableModel);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateTable.setRowSelectionAllowed(true);
        templateTable.setShowGrid(true);
        templateTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        templateTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        templateTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        templateTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        templateTable.getColumnModel().getColumn(3).setPreferredWidth(300);

        // Double-click to edit
        templateTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEditTemplate();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(templateTable);
        root.add(scrollPane, BorderLayout.CENTER);

        // Empty state label
        if (allTemplates.isEmpty()) {
            JLabel emptyLabel = new JBLabel(MessagesBundle.getText("dialog.template-management.empty"), SwingConstants.CENTER);
            emptyLabel.setForeground(Color.GRAY);
            root.add(emptyLabel, BorderLayout.CENTER);
        }

        return root;
    }

    private void filterTable() {
        String query = searchField.getText().trim().toLowerCase();
        List<CredentialTemplate> filtered;
        if (query.isEmpty()) {
            filtered = new ArrayList<>(allTemplates);
        } else {
            filtered = allTemplates.stream()
                    .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }
        tableModel.setData(filtered);
    }

    private void onCreateTemplate() {
        CredentialTemplateEditDialog dialog = new CredentialTemplateEditDialog(project, null);
        if (dialog.showAndGet()) {
            CredentialTemplate created = dialog.getTemplate();
            if (created != null) {
                allTemplates.add(created);
                refreshTable();
            }
        }
    }

    private void onEditTemplate() {
        int selectedRow = templateTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showInfoMessage(
                    project,
                    MessagesBundle.getText("dialog.template-management.error.no-selection"),
                    MessagesBundle.getText("dialog.template-management.error.no-selection.title"));
            return;
        }

        int modelRow = templateTable.convertRowIndexToModel(selectedRow);
        CredentialTemplate template = tableModel.getTemplateAt(modelRow);
        CredentialTemplateEditDialog dialog = new CredentialTemplateEditDialog(project, template);
        if (dialog.showAndGet()) {
            refreshTable();
        }
    }

    private void onDeleteTemplate() {
        int selectedRow = templateTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showInfoMessage(
                    project,
                    MessagesBundle.getText("dialog.template-management.error.no-selection"),
                    MessagesBundle.getText("dialog.template-management.error.no-selection.title"));
            return;
        }

        int modelRow = templateTable.convertRowIndexToModel(selectedRow);
        CredentialTemplate template = tableModel.getTemplateAt(modelRow);

        int result = Messages.showYesNoDialog(
                project,
                MessagesBundle.getText("dialog.template-management.delete.confirm", template.getName()),
                MessagesBundle.getText("dialog.template-management.delete.title"),
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            TemplateManager.getInstance().removeTemplate(template.getUid());
            TemplateAuditLogger.logTemplateDeletion(template.getUid(), template.getName());
            allTemplates.remove(template);
            refreshTable();
        }
    }

    private void refreshTable() {
        allTemplates = new ArrayList<>(TemplateManager.getInstance().getAllTemplates());
        filterTable();
    }

    /**
     * Show the template management dialog.
     *
     * @param project the current project
     */
    public static void show(@Nullable Project project) {
        TemplateManagementDialog dialog = new TemplateManagementDialog(project);
        dialog.show();
    }

    /**
     * Table model for displaying credential templates.
     */
    private static class TemplateTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {
                MessagesBundle.getText("dialog.template-management.columns.name"),
                MessagesBundle.getText("dialog.template-management.columns.username"),
                MessagesBundle.getText("dialog.template-management.columns.auth-type"),
                MessagesBundle.getText("dialog.template-management.columns.description")
        };

        private List<CredentialTemplate> templates;

        TemplateTableModel(List<CredentialTemplate> templates) {
            this.templates = templates != null ? templates : Collections.emptyList();
        }

        void setData(List<CredentialTemplate> templates) {
            this.templates = templates != null ? templates : Collections.emptyList();
            fireTableDataChanged();
        }

        CredentialTemplate getTemplateAt(int row) {
            if (row >= 0 && row < templates.size()) {
                return templates.get(row);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return templates.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int col) {
            CredentialTemplate template = templates.get(row);
            switch (col) {
                case 0:
                    return template.getName();
                case 1:
                    return template.getUsername();
                case 2:
                    Integer authType = template.getAuthType();
                    if (AuthType.PEM_PRIVATE_KEY.getCode().equals(authType)) {
                        return AuthType.PEM_PRIVATE_KEY.getDesc();
                    }
                    return AuthType.PASSWORD.getDesc();
                case 3:
                    return template.getDescription() != null ? template.getDescription() : "";
                default:
                    return "";
            }
        }
    }
}
