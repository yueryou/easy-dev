# Command → Task 改造方案

## 需求
将 Dashboard 页面 "Command" 按钮改为 "Task"，点击后弹出快捷指令和流水线任务面板，双击指令在远程服务器执行命令，双击流水线任务时将该任务中 upload 和 remote_command 步骤的服务器替换为当前选择的服务器。

## 现状分析

| 组件 | 行为 |
|------|------|
| `HostActionPanel.commandBtn` | 点击 → `CommandDialogAction` → `SelectCommandDialog` → 选择命令 → SSH 执行 |
| `QuickCommandAction` (菜单入口) | 点击 → `CommandManageDialog` → Tab1: 快捷指令(发Terminal) + Tab2: 流水线(执行Pipeline) |

## 改造方案

### 架构

```
HostActionPanel (sshId=5)
  └─ TaskPanelDialogAction(sshId=5)
      └─ TaskPanelDialog(sshId=5)
          ├─ CommandManagePanel(sshId=5)  → 双击命令 → CommandUtil.executeCommand(serverId=5)
          └─ CommandPipelinePanel(sshId=5) → 双击流水线 → 替换步骤 serverId=5 → PipelineExecutor
```

### 涉及文件

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `HostActionPanel.java` | 修改 | 按钮文本 + 点击动作 |
| `messages_en.properties` | 修改 | 新增 key |
| `messages_zh.properties` | 修改 | 新增 key |
| `TaskPanelDialogAction.java` | 新增 | 新 Action |
| `TaskPanelDialog.java` | 新增 | 弹窗，组合两个面板并传入 sshId |
| `CommandManagePanel.java` | 修改 | 增加 sshId 构造函数，支持 SSH 直发模式 |
| `CommandPipelinePanel.java` | 修改 | 增加 sshId 构造函数，执行前替换步骤 serverId |

### 核心逻辑

1. **CommandManagePanel SSH 模式**: 传入 sshId 时隐藏 session 复选列表，双击走 SSH 直发（`CommandUtil.executeCommand()`）
2. **CommandPipelinePanel 临时替换**: 执行前遍历步骤，将 `UploadStep` 和 `RemoteCommandStep` 的 `serverId` 临时替换为传入的 sshId，不修改持久化数据
3. **向后兼容**: `CommandManageDialog`（菜单入口）不传 sshId，保持原有 Terminal session 行为

### 风险点

1. `CommandManageDialog` 不能受影响
2. `serverId` 替换是临时的，不应修改持久化
3. `CommandManagePanel` 的 session 复选列表在 SSH 模式下隐藏
