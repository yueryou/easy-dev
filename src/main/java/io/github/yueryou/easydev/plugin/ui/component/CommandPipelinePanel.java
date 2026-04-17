package io.github.yueryou.easydev.plugin.ui.component;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import io.github.yueryou.easydev.plugin.model.Pipeline;
import io.github.yueryou.easydev.plugin.model.PipelineConfigPersistence;
import io.github.yueryou.easydev.plugin.model.StepType;
import io.github.yueryou.easydev.plugin.ui.dialog.PipelineEditDialog;
import io.github.yueryou.easydev.plugin.ui.render.PipelineListCellRenderer;
import org.jetbrains.annotations.NotNull;
import tech.lin2j.idea.plugin.event.ApplicationListener;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.event.PipelineRefreshEvent;
import tech.lin2j.idea.plugin.service.impl.PluginNotificationService;
import tech.lin2j.idea.plugin.ssh.CommandLog;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.ui.module.ConsoleLogView;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;
import tech.lin2j.idea.plugin.uitl.UiUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import com.intellij.execution.ui.ConsoleViewContentType;
import tech.lin2j.idea.plugin.event.ApplicationListener;
import tech.lin2j.idea.plugin.event.ApplicationEvent;
import io.github.yueryou.easydev.plugin.executor.PipelineExecutor;
import io.github.yueryou.easydev.plugin.action.CopyPipelineAction;
import io.github.yueryou.easydev.plugin.action.PastePipelineAction;
import io.github.yueryou.easydev.plugin.log.UnifiedLogger;
import io.github.yueryou.easydev.plugin.model.PipelineResult;

/**
 * 自定义任务流水线面板
 */
public class CommandPipelinePanel extends JPanel implements ApplicationListener<PipelineRefreshEvent> {

    private final Project project;
    private final JPanel root;

    /**
     * 搜索输入框
     */
    private JBTextField searchInput;

    /**
     * 流水线列表
     */
    private JBList<Pipeline> pipelineList;

    private final PluginNotificationService notificationService;
    private final UnifiedLogger logger = UnifiedLogger.getInstance();

    /**
     * 双击执行流水线后的回调（用于关闭弹窗）
     */
    private Runnable onDoubleClickExecute;

    public CommandPipelinePanel(Project project) {
        this.project = project;
        this.notificationService = ApplicationManager.getApplication().getService(PluginNotificationService.class);
        initInput();
        initPipelineList();
        bindInputChangeListener(loadPipelineList());

        root = FormBuilder.createFormBuilder()
                .addLabeledComponent(MessagesBundle.getText("pipeline.search"), searchInput)
                .addComponentFillVertically(createPipelineToolbarPanel(), 8)
                .getPanel();
        root.setPreferredSize(new Dimension(UiUtil.screenWidth() / 2, 600));
    }

    private void initInput() {
        searchInput = new JBTextField();
        searchInput.getEmptyText().setText(MessagesBundle.getText("pipeline.search.placeholder"));
    }

    private CopyPipelineAction copyPipelineAction;
    private PastePipelineAction pastePipelineAction;

    private void initPipelineList() {
        pipelineList = new JBList<>();
        pipelineList.setCellRenderer(new PipelineListCellRenderer());

        // Initialize copy/paste actions with pipeline list selection
        copyPipelineAction = new CopyPipelineAction(() -> pipelineList.getSelectedValue());
        pastePipelineAction = new PastePipelineAction(project);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                Pipeline pipeline = pipelineList.getSelectedValue();
                if (pipeline != null) {
                    // 先捕获 pipeline（对话框关闭前 UI 可用）
                    Pipeline capturedPipeline = pipeline;
                    // 关闭对话框，让焦点回到终端
                    if (onDoubleClickExecute != null) {
                        onDoubleClickExecute.run();
                    }
                    // 对话框关闭后，再执行流水线
                    SwingUtilities.invokeLater(() ->
                        executePipelineAfterDialogClose(capturedPipeline));
                }
                return true;
            }
        }.installOn(pipelineList);
    }

    private JPanel createPipelineToolbarPanel() {
        return ToolbarDecorator.createDecorator(pipelineList)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .disableUpDownActions()
                .setAddAction(e -> showPipelineEditDialog(new Pipeline()))
                .setEditAction(e -> {
                    Pipeline pipeline = pipelineList.getSelectedValue();
                    if (pipeline != null) {
                        showPipelineEditDialog(pipeline);
                    }
                })
                .setRemoveAction(e -> {
                    Pipeline pipeline = pipelineList.getSelectedValue();
                    if (pipeline == null) {
                        return;
                    }
                    boolean confirm = UiUtil.deleteConfirm(pipeline.getName());
                    if (confirm) {
                        PipelineConfigPersistence.removePipeline(pipeline);
                        loadPipelineList();
                    }
                })
                .addExtraAction(copyPipelineAction)
                .addExtraAction(pastePipelineAction)
                .createPanel();
    }

    private void showPipelineEditDialog(Pipeline pipeline) {
        PipelineEditDialog dialog = new PipelineEditDialog(project, pipeline);
        if (dialog.showAndGet()) {
            loadPipelineList();
        }
    }

    @Override
    public void onApplicationEvent(PipelineRefreshEvent event) {
        logger.debug("CommandPipelinePanel", "Received PipelineRefreshEvent, refreshing pipeline list");
        loadPipelineList();
        logger.debug("CommandPipelinePanel", "Pipeline list refreshed successfully");
    }

    private void bindInputChangeListener(List<Pipeline> pipelines) {
        searchInput.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                String text = searchInput.getText();
                if (text.isBlank()) {
                    pipelineList.setListData(pipelines.toArray(new Pipeline[0]));
                } else {
                    List<Pipeline> searchList = pipelines.stream()
                            .filter(pipeline -> pipeline.getName().contains(text))
                            .toList();
                    pipelineList.setListData(searchList.toArray(new Pipeline[0]));
                }
            }
        });
    }

    public List<Pipeline> loadPipelineList() {
        logger.debug("CommandPipelinePanel", "Loading pipeline list");
        List<Pipeline> pipelines = PipelineConfigPersistence.getAllPipelines();
        pipelineList.setListData(pipelines.toArray(new Pipeline[0]));
        logger.debug("CommandPipelinePanel", "Pipeline list loaded with " + (pipelines != null ? pipelines.size() : 0) + " items");
        return pipelines;
    }

    public JPanel createUI() {
        return root;
    }

    /**
     * 设置双击执行流水线后的回调
     * @param callback 回调函数（用于关闭弹窗）
     */
    public void setOnDoubleClickExecute(Runnable callback) {
        this.onDoubleClickExecute = callback;
    }

    /**
     * 执行选中的流水线
     */
    public void executeSelectedPipeline() {
        executePipelineFromStep(0);
    }

    /**
     * 对话框关闭后执行流水线（通过 SwingUtilities.invokeLater 调用）
     */
    private void executePipelineAfterDialogClose(Pipeline pipeline) {
        if (pipeline == null) {
            notificationService.showNotification(project, MessagesBundle.getText("pipeline.notification.title.running"),
                MessagesBundle.getText("pipeline.error.no.selected"));
            return;
        }
        if (pipeline.getPipelineSteps() == null || pipeline.getPipelineSteps().isEmpty()) {
            notificationService.showNotification(project, MessagesBundle.getText("pipeline.notification.title.running"),
                MessagesBundle.getText("pipeline.error.no.steps"));
            return;
        }

        Consumer<String> logConsumer = message -> {
            CommandLog commandLog = project.getUserData(CommandLog.COMMAND_LOG_KEY);
            if (commandLog != null) {
                commandLog.print(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
            }
        };

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            com.intellij.openapi.wm.ToolWindow toolWindow = toolWindowManager.getToolWindow("Easy Dev");
            if (toolWindow != null) {
                toolWindow.show(() -> {
                    var contentManager = toolWindow.getContentManager();
                    var consoleContent = contentManager.findContent("Console");
                    if (consoleContent != null) {
                        contentManager.setSelectedContent(consoleContent);
                        CommandLog commandLog = project.getUserData(CommandLog.COMMAND_LOG_KEY);
                        if (commandLog == null && consoleContent.getComponent() instanceof ConsoleLogView) {
                            ((ConsoleLogView) consoleContent.getComponent()).attachProject();
                        }
                        if (commandLog != null && commandLog.getConsole() != null) {
                            commandLog.getConsole().clear();
                        }
                        executePipelineAsync(pipeline, 0, logConsumer);
                    }
                });
            } else {
                executePipelineAsync(pipeline, 0, logConsumer);
            }
        });
    }

    private void executePipelineFromStep(int startIndex) {
        Pipeline pipeline = pipelineList.getSelectedValue();
        if (pipeline == null) {
            notificationService.showNotification(project, MessagesBundle.getText("pipeline.notification.title.running"),
                MessagesBundle.getText("pipeline.error.no.selected"));
            return;
        }

        if (pipeline.getPipelineSteps() == null || pipeline.getPipelineSteps().isEmpty()) {
            notificationService.showNotification(project, MessagesBundle.getText("pipeline.notification.title.running"),
                MessagesBundle.getText("pipeline.error.no.steps"));
            return;
        }

        // 创建日志消费者
        Consumer<String> logConsumer = message -> {
            // 直接打印到控制台，稍后会显示工具窗口
            CommandLog commandLog = project.getUserData(CommandLog.COMMAND_LOG_KEY);
            if (commandLog != null) {
                commandLog.print(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
            }
        };

        // 先激活 Easy Dev 工具窗口和 Console 标签页，确保控制台已初始化
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            com.intellij.openapi.wm.ToolWindow toolWindow = toolWindowManager.getToolWindow("Easy Dev");
            if (toolWindow != null) {
                toolWindow.show(() -> {
                    // 激活 Console 标签页
                    var contentManager = toolWindow.getContentManager();
                    var consoleContent = contentManager.findContent("Console");
                    if (consoleContent != null) {
                        contentManager.setSelectedContent(consoleContent);
                        // 确保 ConsoleLogView 已附加到项目
                        CommandLog commandLog = project.getUserData(CommandLog.COMMAND_LOG_KEY);
                        if (commandLog == null && consoleContent.getComponent() instanceof ConsoleLogView) {
                            ((ConsoleLogView) consoleContent.getComponent()).attachProject();
                        }
                        // 清空控制台
                        if (commandLog != null && commandLog.getConsole() != null) {
                            commandLog.getConsole().clear();
                        }
                        // 开始执行流水线
                        executePipelineAsync(pipeline, startIndex, logConsumer);
                    }
                });
            } else {
                // 工具窗口不存在，直接执行
                executePipelineAsync(pipeline, startIndex, logConsumer);
            }
        });
    }

    /**
     * 异步执行流水线
     */
    private void executePipelineAsync(Pipeline pipeline, int startIndex, Consumer<String> logConsumer) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, MessagesBundle.getText("pipeline.running") + pipeline.getName()) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    PipelineResult result = PipelineExecutor.executeFromStep(pipeline, null, project, logConsumer, startIndex);

                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    logConsumer.accept("\n========== 流水线完成 " + time + " ==========");

                    // 显示执行结果通知
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String title = result.isSuccess()
                            ? MessagesBundle.getText("pipeline.notification.success.title")
                            : MessagesBundle.getText("pipeline.notification.failure.title");

                        StringBuilder message = new StringBuilder();
                        if (!result.isSuccess() && result.getFailedStep() != null) {
                            message.append(MessagesBundle.getText("pipeline.notification.failure.step"))
                                   .append(result.getFailedStep().getName());
                        } else {
                            message.append(pipeline.getName());
                        }
                        notificationService.showNotificationWithSound(project, title, message.toString());
                    });

                } catch (NumberFormatException e) {
                    logConsumer.accept("服务器 ID 格式错误：" + e.getMessage());
                    notificationService.showSystemTrayErrorNotification(project, MessagesBundle.getText("pipeline.notification.title.running"),
                        MessagesBundle.getText("pipeline.notification.server.id.error") + e.getMessage());
                } catch (Exception e) {
                    logConsumer.accept("执行异常：" + e.getMessage());
                    notificationService.showSystemTrayErrorNotification(project, MessagesBundle.getText("pipeline.notification.title.running"),
                        MessagesBundle.getText("pipeline.notification.execution.error") + e.getMessage());
                }
            }
        });
    }
}
