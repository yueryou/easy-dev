package io.github.yueryou.easydev.plugin.ui.component;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.TerminalView;
import tech.lin2j.idea.plugin.action.CopyCommandAction;
import tech.lin2j.idea.plugin.event.ApplicationListener;
import tech.lin2j.idea.plugin.model.Command;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.SeparatorCommand;
import tech.lin2j.idea.plugin.model.event.CommandAddEvent;
import tech.lin2j.idea.plugin.service.impl.PluginNotificationService;
import tech.lin2j.idea.plugin.ui.dialog.AddCommandDialog;
import tech.lin2j.idea.plugin.ui.dialog.SelectCommandDialog;
import tech.lin2j.idea.plugin.ui.render.CommandColoredListCellRenderer;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;
import tech.lin2j.idea.plugin.uitl.UiUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 命令管理
 * <p>1. 新增、编辑、删除、搜索、详情命令</p>
 * <p>2. 发送命令到session，支持选择一个或者多个session</p>
 * <p>3. 创建新的session并发送命令</p>
 * <p>4. 记录历史命令及操作日志</p>
 */
public class CommandManagePanel extends JPanel implements ApplicationListener<CommandAddEvent> {
    private static final Logger LOG = Logger.getInstance(SelectCommandDialog.class);

    private final JPanel root;
    /**
     * 搜索输入框
     */
    private JBTextField searchInput;
    /**
     * 命令详情
     */
    private JBTextField commandDetails;
    /**
     * 命令列表
     */
    private JBList<Command> commandList;

    private SearchableCheckboxList searchableCheckboxList;

    /**
     * 已选择的命令
     */
    private transient Command selectedCommand;

    private final transient Project project;
    private final PluginNotificationService notificationService;

    /**
     * 双击执行命令后的回调（用于关闭弹窗）
     */
    private Runnable onDoubleClickExecute;

    public CommandManagePanel(Project project) {
        this.project = project;

        initInput();
        initCommandList();
        initCommandDetail();

        // 初始化session列表，获取当前活跃的session。
        List<@Nls @Nullable String> sessionList = getActiveSessionList(project).keySet().stream().toList();
        initSessionCheckableList(sessionList);
        // 绑定监听时间，实现模糊搜索
        bindInputChangeListener(loadCommandList());
        root = FormBuilder.createFormBuilder()
                .addLabeledComponent(MessagesBundle.getText("dialog.command.search.show"), searchInput)
                .addComponentFillVertically(createCommandToolbarPanel(), 8)
                .addLabeledComponent(MessagesBundle.getText("dialog.command.detail"), commandDetails)
                .addLabeledComponent(MessagesBundle.getText("dialog.command.send.hosts"), searchableCheckboxList)
                .getPanel();
        root.setPreferredSize(new Dimension(UiUtil.screenWidth() / 2, 600));
        notificationService = ApplicationManager.getApplication().getService(PluginNotificationService.class);
    }

    private void initSessionCheckableList(List<String> sessionList) {
        searchableCheckboxList = new SearchableCheckboxList(sessionList);
    }

    public Map<@Nls @Nullable String, JBTerminalWidget> getActiveSessionList(Project project) {
        if (project == null) return null;
        TerminalView instance = TerminalView.getInstance(project);
        return instance.getWidgets().stream().collect(Collectors.toMap(item -> {
            return item.getTerminalTitle().getDefaultTitle();
        }, value -> value));
    }

    private void bindInputChangeListener(List<Command> commands) {
        searchInput.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                // 根据查询条件更新命令列表
                // 当输入内容为空时，显示所有命令
                String text = searchInput.getText();
                if (text.isBlank()) {
                    // 重置commonList
                    commandList.setListData(commands.toArray(new Command[0]));
                } else {
                    List<Command> searchList = commands.stream().filter(command -> Strings.contains(command.getTitle(), text)).toList();
                    // 预先构建名称到索引的映射
                    Map<String, Integer> nameToIndex = new HashMap<>();
                    for (int i = 0; i < searchList.size(); i++) {
                        nameToIndex.put(searchList.get(i).getTitle(), i);
                    }

                    // 快速查找
                    Integer index = nameToIndex.get(text);
                    if (index != null) {
                        LOG.info("find command: " + text);
                        commandList.setSelectedIndex(index);
                    }
                    commandList.setListData(searchList.toArray(new Command[0]));
                    commandList.requestFocusInWindow();
                    commandList.repaint();
                }
            }
        });
    }

    @Override
    public void onApplicationEvent(CommandAddEvent event) {
        loadCommandList();
    }

    private void initInput() {
        searchInput = new JBTextField();
    }

    private void initCommandDetail() {
        commandDetails = new JBTextField();
        commandDetails.setEditable(false);
    }

    private void initCommandList() {
        commandList = new JBList<>();
        commandList.setCellRenderer(new CommandColoredListCellRenderer());
        commandList.addListSelectionListener(e -> {
            Command command = commandList.getSelectedValue();
            if (command != null) {
                selectedCommand = command;
                commandDetails.setText(command.toString());
            } else {
                commandDetails.setText("");
            }
        });
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                Command command = commandList.getSelectedValue();
                if (command != null) {
                    if (onDoubleClickExecute != null) {
                        onDoubleClickExecute.run();
                    }
                    executeCommand();
                }
                return true;
            }
        }.installOn(commandList);
    }

    private JPanel createCommandToolbarPanel() {
        return ToolbarDecorator.createDecorator(commandList)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .disableUpDownActions()
                .setEditActionUpdater(e -> isEditable())
                .setRemoveActionUpdater(e -> isEditable())
                .setAddAction(e -> {
                    Command command = new Command();
                    new AddCommandDialog(project, command).showAndGet();
                })
                .setRemoveAction(e -> {
                    Command cmd = commandList.getSelectedValue();
                    if (cmd == null) {
                        return;
                    }
                    boolean confirm = UiUtil.deleteConfirm(cmd.toString());
                    if (confirm) {
                        ConfigHelper.removeCommand(cmd);
                        loadCommandList();
                    }
                })
                .setEditAction(e -> {
                    Command cmd = commandList.getSelectedValue();
                    if (cmd == null) {
                        return;
                    }
                    new AddCommandDialog(project, cmd).showAndGet();
                })
                .addExtraAction(new CopyCommandAction(() -> selectedCommand))
                .createPanel();
    }

    private boolean isEditable() {
        Command selectedValue = commandList.getSelectedValue();
        if (selectedValue instanceof SeparatorCommand) {
            return false;
        }
        if (selectedValue != null) {
            return !selectedValue.getSharable();
        }
        return true;
    }

    public java.util.List<Command> loadCommandList() {
        java.util.List<Command> commands = ConfigHelper.getCommandList();
        commandList.setListData(commands.toArray(new Command[0]));
        return commands;
    }

    public JPanel createUI() {
        return root;
    }

    /**
     * 设置双击执行命令后的回调
     * @param callback 回调函数（用于关闭弹窗）
     */
    public void setOnDoubleClickExecute(Runnable callback) {
        this.onDoubleClickExecute = callback;
    }

    /**
     * 向目标session发送指令
     */
    public void executeCommand() {
        String title = "Send command";
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // 向当前活跃的终端发送指令
                if (project == null) return;
                TerminalView instance = TerminalView.getInstance(project);
                // 获取选中的session列表
                Command selectedValue = commandList.getSelectedValue();
                // 获取要执行的命令
                if (selectedValue == null) {
                    notificationService.showNotification(project, title, "no command selected");
                    return;
                }

                List<String> selectedItems = searchableCheckboxList.getSelectedItems();
                List<JBTerminalWidget> targetWidgets;

                // 如果未选中任何 session，自动获取当前激活的终端窗口
                if (selectedItems.isEmpty()) {
                    JBTerminalWidget activeWidget = findActiveTerminalWidget(instance);
                    if (activeWidget == null) {
                        notificationService.showNotification(project, title, "no active terminal found");
                        return;
                    }
                    targetWidgets = List.of(activeWidget);
                } else {
                    // 根据session name 过滤终端
                    targetWidgets = instance.getWidgets().stream()
                            .filter(it -> selectedItems.contains(it.getTerminalTitle().getDefaultTitle()))
                            .collect(Collectors.toList());
                }

                // 向目标终端发送命令
                for (JBTerminalWidget terminalWidget : targetWidgets) {
                    String sessionName = terminalWidget.getTerminalTitle().getDefaultTitle();
                    TtyConnector ttyConnector = terminalWidget.getTtyConnector();
                    if (ttyConnector != null) {
                        try {
                            String command = selectedValue.generateCmdLine();
                            ttyConnector.write(command + "\r");
                        } catch (IOException e) {
                            String msg = sessionName + " send failed: " + e.getMessage();
                            notificationService.showNotification(project, title, msg);
                        }
                    }
                }
            }
        });
    }

    /**
     * 查找当前激活的终端窗口（焦点所在的终端）
     */
    private JBTerminalWidget findActiveTerminalWidget(TerminalView terminalView) {
        // 尝试通过焦点查找
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JBTerminalWidget focusedWidget = findParentTerminalWidget(focusOwner);
        if (focusedWidget != null) {
            return focusedWidget;
        }

        // 如果焦点查找失败，尝试通过 hasFocus 查找
        for (JBTerminalWidget widget : terminalView.getWidgets()) {
            if (widget.hasFocus()) {
                return widget;
            }
        }

        // 如果仍没找到，返回最后一个终端（通常是最近使用的）
        java.util.Set<JBTerminalWidget> widgets = terminalView.getWidgets();
        if (!widgets.isEmpty()) {
            // 转换为列表并返回最后一个
            List<JBTerminalWidget> widgetList = widgets.stream().collect(Collectors.toList());
            return widgetList.get(widgetList.size() - 1);
        }

        return null;
    }

    /**
     * 从组件向上遍历查找父级 JBTerminalWidget
     */
    private JBTerminalWidget findParentTerminalWidget(Component component) {
        while (component != null) {
            if (component instanceof JBTerminalWidget) {
                return (JBTerminalWidget) component;
            }
            component = component.getParent();
        }
        return null;
    }
}
