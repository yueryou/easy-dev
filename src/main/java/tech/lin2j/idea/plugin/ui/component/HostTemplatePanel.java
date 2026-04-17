package tech.lin2j.idea.plugin.ui.component;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import tech.lin2j.idea.plugin.enums.AuthType;
import tech.lin2j.idea.plugin.model.CredentialTemplate;
import tech.lin2j.idea.plugin.service.TemplateManager;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

public class HostTemplatePanel {

    private final JPanel root;
    private final Project project;
    private SshServer contentProvider;

    private JBRadioButton noTemplateRadio;
    private JBRadioButton useTemplateRadio;
    private ComboBox<CredentialTemplate> templateComboBox;
    private JPanel infoPanel;
    private JBLabel usernameLabel;
    private JBLabel authTypeLabel;

    public HostTemplatePanel(Project project, SshServer contentProvider) {
        this.project = project;
        this.contentProvider = contentProvider;
        if (contentProvider != null) {
            this.contentProvider = contentProvider.clone();
        }

        initRadio();
        initComboBox();
        initInfoPanel();
        setContent();

        root = new JPanel(new GridBagLayout());
        root.add(noTemplateRadio, new GridBagConstraints(0, 0, 1, 1, 1, 0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, null, 0, 0));
        root.add(useTemplateRadio, new GridBagConstraints(0, 1, 1, 1, 1, 0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, null, 0, 0));
        root.add(templateComboBox, new GridBagConstraints(0, 2, 1, 1, 1, 0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, null, 0, 0));
        root.add(infoPanel, new GridBagConstraints(0, 3, 1, 1, 1, 0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, null, 0, 0));
    }

    public JPanel createUI() {
        return root;
    }

    public void setTemplateSettings(SshServer server) {
        if (useTemplateRadio.isSelected()) {
            CredentialTemplate selected = (CredentialTemplate) templateComboBox.getSelectedItem();
            if (selected != null) {
                server.setTemplateId(selected.getUid());
                server.setUseTemplateCredentials(true);
            }
        } else {
            server.setTemplateId(null);
            server.setUseTemplateCredentials(false);
        }
    }

    private void initRadio() {
        noTemplateRadio = new JBRadioButton(MessagesBundle.getText("dialog.panel.host.template.radio.no-template"));
        useTemplateRadio = new JBRadioButton(MessagesBundle.getText("dialog.panel.host.template.radio.use-template"));

        ButtonGroup group = new ButtonGroup();
        group.add(noTemplateRadio);
        group.add(useTemplateRadio);

        noTemplateRadio.addActionListener(e -> {
            templateComboBox.setEnabled(false);
            infoPanel.setVisible(false);
        });
        useTemplateRadio.addActionListener(e -> {
            templateComboBox.setEnabled(true);
            onTemplateSelected();
        });
    }

    private void initComboBox() {
        List<CredentialTemplate> templates = new ArrayList<>(TemplateManager.getInstance().getAllTemplates());
        templateComboBox = new ComboBox<>();
        templateComboBox.setModel(new CollectionComboBoxModel<>(templates));
        templateComboBox.setEnabled(false);
        templateComboBox.addActionListener(e -> onTemplateSelected());
    }

    private void initInfoPanel() {
        infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setVisible(false);

        usernameLabel = new JBLabel();
        authTypeLabel = new JBLabel();

        infoPanel.add(new JBLabel(MessagesBundle.getText("dialog.panel.host.template.username") + " "),
                new GridBagConstraints(0, 0, 1, 1, 0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, null, 0, 0));
        infoPanel.add(usernameLabel,
                new GridBagConstraints(1, 0, 1, 1, 1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, null, 0, 0));
        infoPanel.add(new JBLabel(MessagesBundle.getText("dialog.panel.host.template.auth-type") + " "),
                new GridBagConstraints(0, 1, 1, 1, 0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, null, 0, 0));
        infoPanel.add(authTypeLabel,
                new GridBagConstraints(1, 1, 1, 1, 1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, null, 0, 0));
    }

    private void onTemplateSelected() {
        CredentialTemplate selected = (CredentialTemplate) templateComboBox.getSelectedItem();
        if (selected != null && useTemplateRadio.isSelected()) {
            usernameLabel.setText(selected.getUsername() != null ? selected.getUsername() : "-");
            authTypeLabel.setText(getAuthTypeName(selected.getAuthType()));
            infoPanel.setVisible(true);
        } else {
            infoPanel.setVisible(false);
        }
    }

    private void setContent() {
        if (contentProvider != null && contentProvider.isUseTemplateCredentials() && contentProvider.getTemplateId() != null) {
            useTemplateRadio.setSelected(true);
            useTemplateRadio.doClick();

            CredentialTemplate template = TemplateManager.getInstance().getTemplate(contentProvider.getTemplateId());
            if (template != null) {
                templateComboBox.setSelectedItem(template);
            }
        } else {
            noTemplateRadio.setSelected(true);
            noTemplateRadio.doClick();
        }
    }

    private String getAuthTypeName(Integer authType) {
        if (authType == null) return "-";
        if (AuthType.PASSWORD.getCode().equals(authType)) return AuthType.PASSWORD.getDesc();
        if (AuthType.PEM_PRIVATE_KEY.getCode().equals(authType)) return AuthType.PEM_PRIVATE_KEY.getDesc();
        return "-";
    }
}
