# 实施进度记录

## 2026-04-24

### 初始化
- [x] 创建 task_plan.md — 实施计划总览
- [x] 创建 findings.md — 技术调研发现
- [x] 创建 progress.md — 本文件

### 需求分析
- [x] 分析 DashboardView 结构
- [x] 分析 OpenTerminalAction 终端打开逻辑
- [x] 分析 TerminalRunnerUtil 兼容性处理
- [x] 分析 CommandManagePanel 命令发送机制
- [x] 分析 CommandPipelinePanel 执行流程
- [x] 分析 DeployConsoleToolWindowFactory 标签页结构

### 方案设计
- [x] 确定目标架构（Dashboard + Terminal + Console）
- [x] 评估技术可行性
- [x] 识别风险点（版本兼容性、焦点管理、多终端支持）
- [x] 制定 4 阶段实施计划

---

## Phase 1 详细任务 (待执行)

### 1.1 创建 TerminalTabView
```
新建文件: src/main/java/tech/lin2j/idea/plugin/ui/module/TerminalTabView.java

职责:
- 作为 Terminal 标签页的根容器
- 使用 JBSplitter 实现左右分栏
  - 左侧: Quick Commands + Pipeline 选择区
  - 右侧: 嵌入式终端显示区
- 管理多个 JBTerminalWidget (多 Tab 支持)
- 提供终端开关接口

关键方法:
- addTerminalWidget(JBTerminalWidget widget, String title)
- removeTerminalWidget(JBTerminalWidget widget)
- setActiveTerminal(JBTerminalWidget widget)
```

### 1.2 创建 IntegratedTerminalPanel
```
新建文件: src/main/java/tech/lin2j/idea/plugin/ui/component/IntegratedTerminalPanel.java

职责:
- 封装嵌入式终端的创建和管理
- 复用 TerminalRunnerUtil 创建 CloudTerminalRunner
- 处理终端连接状态

关键方法:
- createTerminal(Project project, SshServer server, String workingDirectory)
- getActiveWidget(): JBTerminalWidget
- getAllWidgets(): List<JBTerminalWidget>
```

### 1.3 修改 DeployConsoleToolWindowFactory
```
修改文件: src/main/java/tech/lin2j/idea/plugin/factory/DeployConsoleToolWindowFactory.java

变更:
1. import 新增:
   - tech.lin2j.idea.plugin.ui.module.TerminalTabView

2. createToolWindowContent 方法变更:
   // 当前顺序: Dashboard(0), Console(1)
   // 新顺序: Dashboard(0), Terminal(1), Console(2)

   DashboardView dashboardView = new DashboardView(project);
   Content deploy = contentFactory.createContent(dashboardView, "Dashboard", false);
   toolWindow.getContentManager().addContent(deploy);

   // 新增 Terminal 标签页
   TerminalTabView terminalTabView = new TerminalTabView(project);
   Content terminal = contentFactory.createContent(terminalTabView, "Terminal", false);
   toolWindow.getContentManager().addContent(terminal);

   ConsoleLogView commandExecuteView = new ConsoleLogView(project);
   Content messages = contentFactory.createContent(commandExecuteView, "Console", false);
   toolWindow.getContentManager().addContent(messages);
```

### 1.4 修改 OpenTerminalAction
```
修改文件: src/main/java/tech/lin2j/idea/plugin/action/OpenTerminalAction.java

变更:
1. 新增构造函数参数或静态工厂方法:
   - openTerminalInIntegratedView(Project, SshServer, workingDirectory)

2. 核心逻辑变更:
   // 当前: terminalView.createNewSession(runner, tabState)
   //      → 在 IDE 原生面板打开终端
   //
   // 新逻辑:
   // 1. 获取 TerminalTabView 实例
   // 2. 调用 terminalTabView.createAndAddTerminal(server)
   // 3. 自动切换到 Terminal 标签页

3. 自动切换标签页:
   toolWindow.getContentManager().setSelectedContent(terminalContent);
```

---

## Phase 2 详细任务 (待执行)

### 2.1 重构 CommandManagePanel
```
文件: src/main/java/io/github/yueryou/easydev/plugin/ui/component/CommandManagePanel.java

变更:
1. 移除 extends DialogWrapper
2. 移除构造函数的 super(project) 调用
3. 移除 setOKButtonText(), setSize(), init() 调用
4. 新增方法:
   - createPanel(): JPanel (替代原来的 createCenterPanel())
   - getRootPanel(): JPanel

5. 命令发送逻辑变更:
   // 旧:
   private void doSendCommand(List<JBTerminalWidget> targetWidgets, Command command) {
       for (JBTerminalWidget terminalWidget : targetWidgets) {
           TtyConnector ttyConnector = terminalWidget.getTtyConnector();
           ttyConnector.write(cmd + "\r");
       }
   }

   // 新: 优先使用集成的终端
   private void doSendCommand(List<JBTerminalWidget> targetWidgets, Command command) {
       // 1. 如果存在集成的终端，优先发送
       JBTerminalWidget integrated = IntegratedTerminalPanel.getActiveInstance(project);
       if (integrated != null) {
           TtyConnector tty = integrated.getTtyConnector();
           tty.write(command.generateCmdLine() + "\r");
           return;
       }
       // 2. 否则回退到 IDE 原生终端
       for (JBTerminalWidget terminalWidget : targetWidgets) {
           TtyConnector ttyConnector = terminalWidget.getTtyConnector();
           ttyConnector.write(command.generateCmdLine() + "\r");
       }
   }
```

### 2.2 调整 CommandManageDialog
```
文件: src/main/java/io/github/yueryou/easydev/plugin/ui/dialog/CommandManageDialog.java

变更:
1. 保留 DialogWrapper 包装 (用于从工具栏打开的场景)
2. 内部使用 CommandManagePanel.createPanel()
3. 考虑添加"在集成终端打开"选项

备选方案:
- 完全移除 CommandManageDialog
- 将 CommandManagePanel 直接嵌入 TerminalTabView
- 工具栏点击时直接显示 TerminalTabView
```

---

## Phase 3 详细任务 (待执行)

### 3.1 重构 CommandPipelinePanel
```
文件: src/main/java/io/github/yueryou/easydev/plugin/ui/component/CommandPipelinePanel.java

变更:
1. 移除 extends DialogWrapper
2. 新增 createPanel(): JPanel 方法
3. 执行结果显示调整:
   // 当前: 输出到 CommandLog (Console 标签页)
   // 新: 输出到集成的终端组件

4. 流水线执行后自动切换到 Terminal 标签页
```

### 3.2 调整 PipelineEditDialog
```
文件: src/main/java/io/github/yueryou/easydev/plugin/ui/dialog/PipelineEditDialog.java

保持不变:
- Pipeline 编辑功能仍使用 DialogWrapper
- 作为编辑详情时的弹窗
```

---

## Phase 4 详细任务 (待执行)

### 4.1 多终端 Tab 支持
```
在 TerminalTabView 中使用 JBTabbedPane:

JBTabbedPane terminalTabs = new JBTabbedPane();
terminalTabs.addTab("Server1:22", widget1);
terminalTabs.addTab("Server2:22", widget2);

splitter.setSecondComponent(terminalTabs);
```

### 4.2 终端关闭功能
```
在每个 Tab 上添加关闭按钮:
- 点击关闭断开 SSH 连接
- 最后一个终端不能关闭 (保留空 Tab)

实现方式:
JBTabbedPane with custom close button on each tab
或使用 JBCloseableTabbedPane (如果可用)
```

### 4.3 布局优化
```
最终布局:

+------------------------------------------+
| [Toolbar: 新建|关闭|设置]                  |
+------------------------------------------+
| [左侧面板 30%]    | [右侧终端面板 70%]     |
| +--------------+ | +------------------+ |
| | Quick Cmd ▼  | | | Server:22       x| |
| | - cmd1       | | | $ _             | |
| | - cmd2       | | |                 | |
| +--------------+ | +------------------+ |
| +--------------+ |                     |
| | Pipeline ▼   | |                     |
| | - pipeline1  | |                     |
| | - pipeline2  | |                     |
| +--------------+ |                     |
+------------------------------------------+
```

---

## 错误记录

暂无错误记录。

---

## 待解决问题

| # | 问题 | 状态 | 备注 |
|---|------|------|------|
| 1 | 多终端 Tab 如何管理 | 待定 | 考虑使用 JBTabbedPane |
| 2 | 终端关闭按钮样式 | 待定 | 需要自定义或找现有 API |
| 3 | IDE Terminal 面板兼容 | 待定 | 保留还是废弃？ |
| 4 | 断线重连机制 | 待定 | 需要实现自动重连？ |
