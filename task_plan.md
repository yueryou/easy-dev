# Terminal 标签页集成实施计划

## 任务目标

将终端功能集成到插件主窗口，在 Dashboard 和 Console 中间新增 Terminal 标签页，统一快捷指令和任务编排功能，提供一致的使用体验。

## 需求背景

### 用户痛点
- 点击 Terminal 按钮后，终端打开在 IDE 原生面板，与插件主窗口不在同一窗口
- Quick Commands 和 Task Pipeline 是对话框形式，与 Terminal 体验不一致
- 需要在多个面板之间切换操作

### 目标架构
```
Easy Dev ToolWindow
├── Dashboard (索引0) — 服务器列表、SSH连接管理
├── Terminal (新增，索引1) — 集成终端 + Quick Commands + Pipeline
└── Console (索引2) — 命令执行日志
```

---

## 实施阶段

### Phase 1: TerminalTabView 基础框架
**目标:** 创建可嵌入的终端面板

- [ ] 1.1 创建 `TerminalTabView extends SimpleToolWindowPanel`
- [ ] 1.2 复用 `TerminalRunnerUtil.createCloudTerminalRunner()` 创建嵌入式终端
- [ ] 1.3 在 `DeployConsoleToolWindowFactory` 中添加 Terminal 标签页
- [ ] 1.4 处理终端 Tab 切换和关闭逻辑

### Phase 2: 迁移 Quick Commands
**目标:** 将 CommandManagePanel 从对话框改为嵌入式面板

- [ ] 2.1 将 `CommandManagePanel` 从 `DialogWrapper` 改为 `JPanel`
- [ ] 2.2 创建新的 `TerminalCommandDialog`（可选，保留原功能）
- [ ] 2.3 修改命令发送逻辑，发送到集成的终端而非 IDE Terminal 面板
- [ ] 2.4 在 TerminalTabView 中集成 CommandManagePanel

### Phase 3: 迁移 Task Pipeline
**目标:** 将 CommandPipelinePanel 从对话框改为嵌入式面板

- [ ] 3.1 将 `CommandPipelinePanel` 从对话框改为可嵌入面板
- [ ] 3.2 修改执行结果输出到集成的终端标签页
- [ ] 3.3 在 TerminalTabView 中集成 CommandPipelinePanel

### Phase 4: UI 优化
**目标:** 优化布局和交互体验

- [ ] 4.1 使用 JBSplitter 实现左右分栏布局
- [ ] 4.2 支持多终端 Tab 切换
- [ ] 4.3 优化终端连接状态显示
- [ ] 4.4 添加终端关闭按钮

---

## 技术决策记录

### 决策 1: 终端嵌入方式
- **选择:** 复用 `TerminalRunnerUtil`，通过 `TerminalView.createNewSession()` 但在 ToolWindow 内展示
- **理由:** 最大程度复用现有代码，减少兼容性风险

### 决策 2: Quick Commands 发送目标
- **旧逻辑:** 发送给 `TerminalView.getWidgets()` 中的活跃终端
- **新逻辑:** 发送给集成的 `JBTerminalWidget` 直接组件
- **理由:** 解耦对 IDE Terminal 面板的依赖

---

## 文件清单

### 新增文件
- `src/main/java/tech/lin2j/idea/plugin/ui/module/TerminalTabView.java` — 终端标签页主视图
- `src/main/java/tech/lin2j/idea/plugin/ui/component/IntegratedTerminalPanel.java` — 嵌入式终端面板

### 修改文件
- `src/main/java/tech/lin2j/idea/plugin/factory/DeployConsoleToolWindowFactory.java` — 添加 Terminal 标签页
- `src/main/java/io/github/yueryou/easydev/plugin/ui/component/CommandManagePanel.java` — 移除 DialogWrapper 依赖
- `src/main/java/io/github/yueryou/easydev/plugin/ui/component/CommandPipelinePanel.java` — 移除 DialogWrapper 依赖
- `src/main/java/tech/lin2j/idea/plugin/action/OpenTerminalAction.java` — 调整终端打开行为

---

## 里程碑

| 阶段 | 完成标准 |
|------|----------|
| Phase 1 | 终端面板可显示，点击可连接 SSH 服务器 |
| Phase 2 | Quick Commands 可发送命令到集成终端 |
| Phase 3 | Task Pipeline 执行结果在集成终端显示 |
| Phase 4 | UI 交互流畅，多 Tab 支持正常 |

---

*创建时间: 2026-04-24*
