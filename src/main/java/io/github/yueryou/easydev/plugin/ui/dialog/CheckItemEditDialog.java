package io.github.yueryou.easydev.plugin.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import io.github.yueryou.easydev.plugin.model.CheckItemType;
import io.github.yueryou.easydev.plugin.model.DelayCheckItem;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 检测项编辑对话框
 */
public class CheckItemEditDialog extends DialogWrapper {

    private final Project project;
    private final DelayCheckItem existingItem;

    private JComboBox<CheckItemType> typeComboBox;
    private JTextField nameField;

    // Remote command fields
    private JComboBox<String> remoteServerComboBox;
    private JTextField remoteCommandField;

    // Script fields
    private JTextField scriptPathField;
    private JComboBox<String> scriptServerComboBox;

    // HTTP fields
    private JTextField httpUrlField;
    private JComboBox<String> httpMethodComboBox;
    private JTextField httpBodyField;
    private JTextField httpExpectedCodeField;

    // Port probe fields
    private JTextField portHostField;
    private JTextField portField;

    // Common timeout
    private JTextField timeoutField;

    // Expected output fields (one per type to avoid shared instance bug)
    private JTextField remoteCommandExpectedOutputField;
    private JTextField localScriptExpectedOutputField;
    private JTextField remoteScriptExpectedOutputField;
    private JTextField httpExpectedOutputField;

    private JPanel cardsPanel;
    private CardLayout cardLayout;

    private DelayCheckItem itemResult;

    public CheckItemEditDialog(Project project, DelayCheckItem existingItem) {
        super(project);
        this.project = project;
        this.existingItem = existingItem;

        setTitle(existingItem != null ? "编辑检测项" : "添加检测项");

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        nameField = new JTextField(30);
        typeComboBox = new JComboBox<>(CheckItemType.values());
        typeComboBox.addActionListener(e -> onTypeChanged());

        timeoutField = new JTextField("10", 10);

        // 远程命令组件
        remoteCommandField = new JTextField(30);
        List<String> serverItems = buildServerItems();
        remoteServerComboBox = new JComboBox<>(serverItems.toArray(new String[0]));

        // 脚本组件
        scriptPathField = new JTextField(30);
        scriptServerComboBox = new JComboBox<>(serverItems.toArray(new String[0]));

        // HTTP 组件
        httpUrlField = new JTextField(30);
        httpMethodComboBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        httpBodyField = new JTextField(30);
        httpExpectedCodeField = new JTextField("200", 10);

        // 端口探测组件
        portHostField = new JTextField(20);
        portField = new JTextField(5);

        // 卡片面板
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        cardsPanel.add(createRemoteCommandPanel(), "REMOTE_COMMAND");
        cardsPanel.add(createLocalScriptPanel(), "LOCAL_SCRIPT");
        cardsPanel.add(createRemoteScriptPanel(), "REMOTE_SCRIPT");
        cardsPanel.add(createHttpRequestPanel(), "HTTP_REQUEST");
        cardsPanel.add(createPortProbePanel(), "PORT_PROBE");

        if (existingItem != null) {
            nameField.setText(existingItem.getName());
            typeComboBox.setSelectedItem(existingItem.getType());
            populateFieldsFromItem(existingItem);
        }

        onTypeChanged();

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("检测类型", typeComboBox)
                .addLabeledComponent("名称", nameField)
                .addLabeledComponent("配置", cardsPanel, true)
                .addLabeledComponent("超时 (秒)", timeoutField)
                .getPanel();
    }

    private List<String> buildServerItems() {
        List<String> items = new ArrayList<>();
        items.add("");
        for (SshServer server : ConfigHelper.sshServers()) {
            items.add(server.getId() + " - " + server.getIp() + ":" + server.getPort());
        }
        return items;
    }

    private JPanel createRemoteCommandPanel() {
        remoteCommandExpectedOutputField = new JTextField(30);
        JLabel tipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("服务器", remoteServerComboBox)
                .addLabeledComponent("命令", remoteCommandField)
                .addLabeledComponent("期望输出包含", remoteCommandExpectedOutputField)
                .addComponent(tipLabel)
                .getPanel();
    }

    private JPanel createLocalScriptPanel() {
        localScriptExpectedOutputField = new JTextField(30);
        JLabel scriptTipLabel = new JLabel("<html><font color='#808080'>支持 .sh/.bash/.bat/.cmd/.ps1/.py，自动识别解释器</font></html>");
        JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("脚本路径", scriptPathField)
                .addLabeledComponent("期望输出包含", localScriptExpectedOutputField)
                .addComponent(scriptTipLabel)
                .addComponent(outputTipLabel)
                .getPanel();
    }

    private JPanel createRemoteScriptPanel() {
        remoteScriptExpectedOutputField = new JTextField(30);
        JLabel scriptTipLabel = new JLabel("<html><font color='#808080'>通过 bash 执行远程脚本</font></html>");
        JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("服务器", scriptServerComboBox)
                .addLabeledComponent("远程脚本路径", scriptPathField)
                .addLabeledComponent("期望输出包含", remoteScriptExpectedOutputField)
                .addComponent(scriptTipLabel)
                .addComponent(outputTipLabel)
                .getPanel();
    }

    private JPanel createHttpRequestPanel() {
        httpExpectedOutputField = new JTextField(30);
        JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查响应内容</font></html>");
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("URL", httpUrlField)
                .addLabeledComponent("方法", httpMethodComboBox)
                .addLabeledComponent("请求体 (POST)", httpBodyField)
                .addLabeledComponent("期望状态码", httpExpectedCodeField)
                .addLabeledComponent("期望输出包含", httpExpectedOutputField)
                .addComponent(outputTipLabel)
                .getPanel();
    }

    private JPanel createPortProbePanel() {
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        portPanel.add(portHostField);
        portPanel.add(new JLabel(":"));
        portPanel.add(portField);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("目标地址", portPanel)
                .getPanel();
    }

    private void onTypeChanged() {
        CheckItemType selectedType = (CheckItemType) typeComboBox.getSelectedItem();
        if (selectedType != null) {
            cardLayout.show(cardsPanel, selectedType.name());
        }
    }

    private void populateFieldsFromItem(DelayCheckItem item) {
        timeoutField.setText(String.valueOf(item.getTimeout()));

        switch (item.getType()) {
            case REMOTE_COMMAND:
                remoteCommandField.setText(item.getCommand());
                selectItemInComboBox(remoteServerComboBox, item.getServerId());
                remoteCommandExpectedOutputField.setText(item.getExpectedOutput());
                break;
            case LOCAL_SCRIPT:
                scriptPathField.setText(item.getScriptPath());
                localScriptExpectedOutputField.setText(item.getExpectedOutput());
                break;
            case REMOTE_SCRIPT:
                scriptPathField.setText(item.getScriptPath());
                selectItemInComboBox(scriptServerComboBox, item.getServerId());
                remoteScriptExpectedOutputField.setText(item.getExpectedOutput());
                break;
            case HTTP_REQUEST:
                httpUrlField.setText(item.getUrl());
                httpMethodComboBox.setSelectedItem(item.getHttpMethod());
                httpBodyField.setText(item.getHttpBody());
                httpExpectedCodeField.setText(String.valueOf(item.getExpectedStatusCode()));
                httpExpectedOutputField.setText(item.getExpectedOutput());
                break;
            case PORT_PROBE:
                portHostField.setText(item.getHost());
                portField.setText(String.valueOf(item.getPort()));
                break;
        }
    }

    private void selectItemInComboBox(JComboBox<String> comboBox, String id) {
        if (id == null || id.isEmpty()) return;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            String item = comboBox.getItemAt(i);
            if (item != null && item.startsWith(id + " - ")) {
                comboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private String extractIdFromComboBoxItem(String item) {
        if (item == null || item.isEmpty()) return null;
        int idx = item.indexOf(" - ");
        return idx > 0 ? item.substring(0, idx) : null;
    }

    @Override
    protected void doOKAction() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    getContentPane(),
                    "请输入检测项名称",
                    "验证失败",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        DelayCheckItem item = createItemFromFields();
        if (item == null) {
            return;
        }
        item.setName(name.trim());

        this.itemResult = item;
        super.doOKAction();
    }

    private DelayCheckItem createItemFromFields() {
        CheckItemType type = (CheckItemType) typeComboBox.getSelectedItem();
        DelayCheckItem item = new DelayCheckItem(type, "");

        // Parse timeout
        try {
            int timeout = Integer.parseInt(timeoutField.getText().trim());
            item.setTimeout(timeout > 0 ? timeout : 10);
        } catch (NumberFormatException e) {
            item.setTimeout(10);
        }

        switch (type) {
            case REMOTE_COMMAND:
                String cmd = remoteCommandField.getText().trim();
                if (cmd.isEmpty()) {
                    showError("请输入远程命令");
                    return null;
                }
                item.setCommand(cmd);
                item.setServerId(extractIdFromComboBoxItem((String) remoteServerComboBox.getSelectedItem()));
                item.setExpectedOutput(remoteCommandExpectedOutputField.getText());
                break;
            case LOCAL_SCRIPT:
                String localScript = scriptPathField.getText().trim();
                if (localScript.isEmpty()) {
                    showError("请输入脚本路径");
                    return null;
                }
                item.setScriptPath(localScript);
                item.setExpectedOutput(localScriptExpectedOutputField.getText());
                break;
            case REMOTE_SCRIPT:
                String remoteScript = scriptPathField.getText().trim();
                if (remoteScript.isEmpty()) {
                    showError("请输入远程脚本路径");
                    return null;
                }
                item.setScriptPath(remoteScript);
                item.setServerId(extractIdFromComboBoxItem((String) scriptServerComboBox.getSelectedItem()));
                item.setExpectedOutput(remoteScriptExpectedOutputField.getText());
                break;
            case HTTP_REQUEST:
                String url = httpUrlField.getText().trim();
                if (url.isEmpty()) {
                    showError("请输入 URL");
                    return null;
                }
                item.setUrl(url);
                item.setHttpMethod((String) httpMethodComboBox.getSelectedItem());
                item.setHttpBody(httpBodyField.getText());
                try {
                    item.setExpectedStatusCode(Integer.parseInt(httpExpectedCodeField.getText().trim()));
                } catch (NumberFormatException e) {
                    item.setExpectedStatusCode(200);
                }
                item.setExpectedOutput(httpExpectedOutputField.getText());
                break;
            case PORT_PROBE:
                String host = portHostField.getText().trim();
                if (host.isEmpty()) {
                    showError("请输入主机地址");
                    return null;
                }
                item.setHost(host);
                try {
                    item.setPort(Integer.parseInt(portField.getText().trim()));
                } catch (NumberFormatException e) {
                    showError("端口号必须是数字");
                    return null;
                }
                break;
        }

        return item;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                getContentPane(),
                message,
                "验证失败",
                JOptionPane.WARNING_MESSAGE
        );
    }

    public DelayCheckItem getItem() {
        return itemResult;
    }
}
