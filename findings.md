# 技术调研发现

## 1. 当前架构分析

### 1.1 DeployConsoleToolWindowFactory 结构

```java
// DeployConsoleToolWindowFactory.java
public class DeployConsoleToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Dashboard tab
        DashboardView dashboardView = new DashboardView(project);
        Content deploy = contentFactory.createContent(dashboardView, "Dashboard", false);
        toolWindow.getContentManager().addContent(deploy);

        // Console tab
        ConsoleLogView commandExecuteView = new ConsoleLogView(project);
        Content messages = contentFactory.createContent(commandExecuteView, "Console", false);
        toolWindow.getContentManager().addContent(messages);
    }
}
```

### 1.2 终端打开机制 (OpenTerminalAction)

```java
// OpenTerminalAction.java
public void openTerminal() {
    SshServer tmp = ConfigHelper.getSshServerById(sshId);
    List<String> ipList = tmp.getIpList();

    // 单个 IP 直接打开，多个 IP 弹出选择对话框
    if (ipList.size() == 1) {
        openTerminalForIp(ipList.get(0), tmp, project, workingDirectory);
    } else {
        showMultiIpDialog(ipList, tmp);
    }
}

private void openTerminalForIp(...) {
    // 通过 TerminalRunnerUtil 创建 CloudTerminalRunner
    runner = TerminalRunnerUtil.createCloudTerminalRunner(project, server, workingDirectory);

    // 使用 TerminalView 在 IDE 原生面板创建终端 Session
    TerminalView terminalView = TerminalView.getInstance(project);
    TerminalTabState tabState = new TerminalTabState();
    tabState.myTabName = ip + ":" + originalServer.getPort();
    terminalView.createNewSession(runner, tabState);
}
```

### 1.3 TerminalRunnerUtil 兼容性处理

```java
// TerminalRunnerUtil.java
private static final Class<?>[] NORMAL_VERSION = {
    Project.class, String.class, CloudTerminalProcess.class,
    TerminalListener.TtyResizeHandler.class, boolean.class
};

private static final Class<?>[] __233_VERSION = {
    Project.class, String.class, CloudTerminalProcess.class,
    TerminalListener.TtyResizeHandler.class  // 无 boolean 参数
};

public static CloudTerminalRunner createCloudTerminalRunner(...) {
    // 尝试正常版本
    Constructor<CloudTerminalRunner> constructor =
        CloudTerminalRunner.class.getConstructor(NORMAL_VERSION);
    instance = constructor.newInstance(project, pipeName, process, resizeHandler, true);

    // 失败则尝试 233 版本
    if (instance == null) {
        Constructor<CloudTerminalRunner> constructor233 =
            CloudTerminalRunner.class.getConstructor(__233_VERSION);
        instance = constructor233.newInstance(project, pipeName, process, resizeHandler);
    }
}
```

### 1.4 CommandManagePanel 当前实现

```java
// CommandManagePanel.java - 关键方法
public Map<@Nls @Nullable String, JBTerminalWidget> getActiveSessionList(Project project) {
    TerminalView instance = TerminalView.getInstance(project);
    return instance.getWidgets().stream()
        .collect(Collectors.toMap(
            item -> item.getTerminalTitle().getDefaultTitle(),
            value -> value
        ));
}

private void doSendCommand(List<JBTerminalWidget> targetWidgets, Command command) {
    for (JBTerminalWidget terminalWidget : targetWidgets) {
        TtyConnector ttyConnector = terminalWidget.getTtyConnector();
        if (ttyConnector != null) {
            String cmd = command.generateCmdLine();
            ttyConnector.write(cmd + "\r");
        }
    }
}
```

### 1.5 CommandPipelinePanel 执行流程

```java
// CommandPipelinePanel.java
private void executePipelineFromStep(int startIndex) {
    // 激活 Easy Dev 工具窗口和 Console 标签页
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    com.intellij.openapi.wm.ToolWindow toolWindow =
        toolWindowManager.getToolWindow("Easy Dev");

    toolWindow.show(() -> {
        var contentManager = toolWindow.getContentManager();
        var consoleContent = contentManager.findContent("Console");
        if (consoleContent != null) {
            contentManager.setSelectedContent(consoleContent);
            // 执行流水线
            executePipelineAsync(pipeline, startIndex, logConsumer);
        }
    });
}

private void executePipelineAsync(Pipeline pipeline, int startIndex,
                                   Consumer<String> logConsumer) {
    // 日志输出到 CommandLog
    Consumer<String> logConsumer = message -> {
        CommandLog commandLog = project.getUserData(CommandLog.COMMAND_LOG_KEY);
        if (commandLog != null) {
            commandLog.print(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
        }
    };
}
```

---

## 2. 关键技术点

### 2.1 IntelliJ Terminal API

| 类/方法 | 用途 |
|---------|------|
| `TerminalView.getInstance(project)` | 获取终端视图实例 |
| `TerminalView.createNewSession(runner, tabState)` | 创建新终端 Session |
| `TerminalView.getWidgets()` | 获取所有终端 Widget |
| `JBTerminalWidget` | 终端 Widget，可获取 TtyConnector |
| `CloudTerminalRunner` | 云终端运行器 |

### 2.2 ToolWindow Content 管理

```java
ContentFactory contentFactory = ContentFactory.getInstance();
Content content = contentFactory.createContent(component, "TabName", false);
toolWindow.getContentManager().addContent(content);
toolWindow.getContentManager().setSelectedContent(content);
```

### 2.3 JBSplitter 分栏布局

```java
JBSplitter splitter = new JBSplitter(false);
splitter.setSplitterProportionKey("terminal.splitter.key");
splitter.setFirstComponent(leftPanel);  // 命令/流水线列表
splitter.setSecondComponent(rightPanel); // 嵌入式终端
splitter.setProportion(0.3f);
```

---

## 3. 风险评估

### 3.1 CloudTerminalRunner 版本兼容性

| IDEA 版本 | 构造函数签名 | 状态 |
|-----------|-------------|------|
| 2022.3 (233) | `(Project, String, CloudTerminalProcess, TerminalListener.TtyResizeHandler)` | 已验证 |
| 2023.x+ | `(Project, String, CloudTerminalProcess, TerminalListener.TtyResizeHandler, boolean)` | 已验证 |

### 3.2 终端焦点管理

当终端被集成到 ToolWindow 后，`CommandManagePanel.doSendCommand()` 需要能找到集成的终端而非 IDE 原生终端。

**解决方案:** 维护一个 `Map<Project, JBTerminalWidget>` 跟踪集成的终端

### 3.3 多终端支持

如果用户连接多个服务器，需要支持多 Tab。

**解决方案:** 在 TerminalTabView 中使用 `JBTabbedPane` 管理多个 `JBTerminalWidget`

---

## 4. 参考资料

- IntelliJ Platform SDK: ToolWindow 指南
- JetBrains/terminal: CloudTerminalRunner 源码
- 现有项目中的 TerminalRunnerUtil 实现

---

*调研时间: 2026-04-24*
