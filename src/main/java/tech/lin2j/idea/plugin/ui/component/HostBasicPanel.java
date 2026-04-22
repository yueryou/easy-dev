package tech.lin2j.idea.plugin.ui.component;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import tech.lin2j.idea.plugin.enums.AuthType;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.CredentialTemplate;
import tech.lin2j.idea.plugin.service.TemplateManager;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.ui.dialog.CredentialTemplateEditDialog;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 * @author lin2j
 * @date 2024-07-21 21:42
 */
public class HostBasicPanel {

    private final JPanel root;

    private JBTextField ipInput;
    private JBTextField portInput;
    private JBTextField descInput;
    private ComboBox<CredentialTemplate> oneKeyComboBox;
    private ComboBox<String> tagComboBox;
    private JPanel testConnectContainer;
    private JPanel oneKeyPanel;
    private JButton addOneKeyButton;
    private JBLabel templateInfoLabel;

    private final SshServer contentProvider;
    private final Project project;
    private final JButton testButton;

    public HostBasicPanel(Project project, SshServer contentProvider, JButton testButton) {
        this.contentProvider = contentProvider;
        this.project = project;
        this.testButton = testButton;

        initInput();
        initOneKeyPanel();
        initTemplateInfoLabel();
        initTagComboBox();
        initTestConnectContainer();
        setContent();

        root = FormBuilder.createFormBuilder()
                .addLabeledComponent(MessagesBundle.getText("dialog.panel.host.basic.ip"), ipInput)
                .addLabeledComponent(MessagesBundle.getText("dialog.panel.host.basic.port"), portInput)
                .addLabeledComponent(MessagesBundle.getText("dialog.panel.host.basic.auth-type"), oneKeyPanel)
                .addComponent(templateInfoLabel)
                .addLabeledComponent(MessagesBundle.getText("dialog.panel.host.basic.tag"), tagComboBox)
                .addLabeledComponent(MessagesBundle.getText("dialog.panel.host.basic.description"), descInput)
                .addComponent(testConnectContainer)
                .getPanel();
    }

    public JPanel createUI() {
        return root;
    }

    /**
     * save server information from panel
     *
     * @return return false when required field is blank
     */
    public boolean saveServerInfo(SshServer server, boolean genId) {
        return !setServerInfo(server, genId);
    }

    private void initInput() {
        ipInput = new JBTextField();
        portInput = new JBTextField();
        descInput = new JBTextField();
    }

    private void initOneKeyPanel() {
        oneKeyComboBox = new ComboBox<>();
        List<CredentialTemplate> templates = new ArrayList<>();
        templates.add(null);
        templates.addAll(TemplateManager.getInstance().getAllTemplates());
        oneKeyComboBox.setModel(new CollectionComboBoxModel<>(templates));

        // noinspection rawtypes
        oneKeyComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText(MessagesBundle.getText("dialog.panel.host.basic.onekey.none"));
                } else {
                    setText(((CredentialTemplate) value).getName());
                }
                return this;
            }
        });

        addOneKeyButton = new JButton(MessagesBundle.getText("dialog.panel.host.basic.onekey.add"));
        addOneKeyButton.addActionListener(e -> {
            CredentialTemplateEditDialog dialog = new CredentialTemplateEditDialog(project, null);
            dialog.show();
            if (dialog.isOK() && dialog.getTemplate() != null) {
                //noinspection unchecked
                CollectionComboBoxModel<CredentialTemplate> model = (CollectionComboBoxModel<CredentialTemplate>) oneKeyComboBox.getModel();
                model.add(dialog.getTemplate());
                oneKeyComboBox.setSelectedItem(dialog.getTemplate());
            }
        });

        oneKeyComboBox.addActionListener(e -> onOneKeyTemplateSelected());

        oneKeyPanel = new JPanel(new BorderLayout());
        oneKeyPanel.add(oneKeyComboBox, BorderLayout.CENTER);
        oneKeyPanel.add(addOneKeyButton, BorderLayout.EAST);
    }

    private void initTemplateInfoLabel() {
        templateInfoLabel = new JBLabel("");
        templateInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 25, 4, 0));
    }

    private void onOneKeyTemplateSelected() {
        CredentialTemplate selected = (CredentialTemplate) oneKeyComboBox.getSelectedItem();
        if (selected != null) {
            String authDesc = "";
            if (selected.getAuthType() != null) {
                if (AuthType.needPassword(selected.getAuthType())) {
                    authDesc = MessagesBundle.getText("dialog.panel.host.basic.auth-type.password");
                } else {
                    authDesc = MessagesBundle.getText("dialog.panel.host.basic.auth-type.private");
                }
            }
            templateInfoLabel.setText(MessagesBundle.getText(
                    "dialog.panel.host.basic.onekey.info",
                    selected.getUsername(),
                    authDesc));
        } else {
            templateInfoLabel.setText("");
        }
    }

    private void initTagComboBox() {
        tagComboBox = new ComboBox<>();
        List<String> tags = new ArrayList<>();
        tags.add("");
        tags.addAll(ConfigHelper.getServerTags());
        tagComboBox.setModel(new CollectionComboBoxModel<>(tags));
    }

    private void initTestConnectContainer() {
        testConnectContainer = new JPanel(new BorderLayout());
        testConnectContainer.add(testButton, BorderLayout.EAST);
    }

    private void setContent() {
        if (contentProvider != null) {
            String ipText = String.join(", ", contentProvider.getIpList());
            ipInput.setText(ipText);
            portInput.setText(contentProvider.getPort().toString());
            tagComboBox.setSelectedItem(contentProvider.getTag());
            descInput.setText(contentProvider.getDescription());

            // Restore template selection
            if (contentProvider.getTemplateId() != null && !contentProvider.getTemplateId().isEmpty()) {
                List<CredentialTemplate> templates = TemplateManager.getInstance().getAllTemplates();
                for (CredentialTemplate t : templates) {
                    if (t.getUid().equals(contentProvider.getTemplateId())) {
                        oneKeyComboBox.setSelectedItem(t);
                        break;
                    }
                }
            }
        }
    }

    /**
     * return true if parameter required is missing
     */
    private boolean setServerInfo(SshServer server, boolean genId) {
        boolean miss = false;
        if (genId) {
            server.setId(ConfigHelper.maxSshServerId() + 1);
            server.setUid(UUID.randomUUID().toString());
        }
        if (setText(ipInput, true, ipString -> {
            server.setIp(ipString);
            if (ipString.contains(",")) {
                List<String> ipList = Arrays.stream(ipString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                server.setIps(ipList);
            } else {
                server.setIps(null);
            }
        })) {
            return true;
        }
        if (setText(portInput, true, port -> server.setPort(Integer.parseInt(port)))) {
            return true;
        }

        // Require a template to be selected
        CredentialTemplate selected = (CredentialTemplate) oneKeyComboBox.getSelectedItem();
        if (selected == null) {
            return true;
        }

        server.setTemplateId(selected.getUid());
        server.setUseTemplateCredentials(true);
        server.setUsername(selected.getUsername());

        if (AuthType.needPassword(selected.getAuthType())) {
            server.setAuthType(AuthType.PASSWORD.getCode());
            server.setPemPrivateKey(null);
            server.setPassPhrase(null);
        } else {
            server.setAuthType(AuthType.PEM_PRIVATE_KEY.getCode());
            server.setPassword(null);
        }

        setText(descInput, false, server::setDescription);
        server.setTag(Objects.toString(tagComboBox.getSelectedItem()));
        return miss;
    }

    /**
     * return true if the text obtained from the input is empty
     */
    private boolean setText(JTextField input, boolean required, Consumer<String> action) {
        boolean requestFocus = false;
        String text = input.getText();
        if (required && StringUtil.isEmpty(text)) {
            SwingUtilities.invokeLater(input::requestFocus);
            requestFocus = true;
        }
        action.accept(text);
        return requestFocus;
    }
}
