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

    // HTTP fields
    private JTextField httpUrlField;
    private JComboBox<String> httpMethodComboBox;
    private JTextField httpBodyField;
    private JTextField httpExpectedCodeField;

    // Common timeout
    private JTextField timeoutField;

    // Expected output fields (one per type to avoid shared instance bug)
    private JTextField remoteCommandExpectedOutputField;
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
        typeComboBox = new JComboBox<>(new CheckItemType[]{
                CheckItemType.REMOTE_COMMAND,
                CheckItemType.HTTP_REQUEST
        });
        typeComboBox.addActionListener(e -> onTypeChanged());

        timeoutField = new JTextField("10", 10);

        // 远程命令组件
        remoteCommandField = new JTextField(30);
        List<String> serverItems = buildServerItems();
        remoteServerComboBox = new JComboBox<>(serverItems.toArray(new String[0]));

        // HTTP 组件
        httpUrlField = new JTextField(30);
        httpMethodComboBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        httpBodyField = new JTextField(30);
        httpExpectedCodeField = new JTextField("200", 10);

        // 卡片面板
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        cardsPanel.add(createRemoteCommandPanel(), "REMOTE_COMMAND");
        cardsPanel.add(createHttpRequestPanel(), "HTTP_REQUEST");

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

    private JPanel createHttpRequestPanel() {
        httpExpectedOutputField = new JTextField(30);
        JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查响应内容（HTTP 状态码始终强制校验）</font></html>");
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("URL", httpUrlField)
                .addLabeledComponent("方法", httpMethodComboBox)
                .addLabeledComponent("请求体 (POST)", httpBodyField)
                .addLabeledComponent("期望状态码 *", httpExpectedCodeField)
                .addLabeledComponent("期望输出包含", httpExpectedOutputField)
                .addComponent(outputTipLabel)
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
            case HTTP_REQUEST:
                httpUrlField.setText(item.getUrl());
                httpMethodComboBox.setSelectedItem(item.getHttpMethod());
                httpBodyField.setText(item.getHttpBody());
                httpExpectedCodeField.setText(String.valueOf(item.getExpectedStatusCode()));
                httpExpectedOutputField.setText(item.getExpectedOutput());
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
            case HTTP_REQUEST:
                String url = httpUrlField.getText().trim();
                if (url.isEmpty()) {
                    showError("请输入 URL");
                    return null;
                }
                String statusCodeText = httpExpectedCodeField.getText().trim();
                if (statusCodeText.isEmpty()) {
                    showError("请输入期望状态码");
                    return null;
                }
                try {
                    int expectedCode = Integer.parseInt(statusCodeText);
                    if (expectedCode < 100 || expectedCode > 599) {
                        showError("状态码必须是 100-599 之间的有效 HTTP 状态码");
                        return null;
                    }
                    item.setExpectedStatusCode(expectedCode);
                } catch (NumberFormatException e) {
                    showError("状态码必须是有效数字");
                    return null;
                }
                item.setUrl(url);
                item.setHttpMethod((String) httpMethodComboBox.getSelectedItem());
                item.setHttpBody(httpBodyField.getText());
                item.setExpectedOutput(httpExpectedOutputField.getText());
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
