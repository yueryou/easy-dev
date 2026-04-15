# 延迟检查任务输出内容检查设计

## 概述

为延迟检查任务的检测项增加输出内容检查能力。当检测项执行成功后，进一步检查输出内容是否包含期望字符串，只有包含期望字符串才认为检测项通过。

## 需求

- 支持配置期望输出字符串（不区分大小写匹配）
- 命令执行成功（exit code = 0）且输出包含期望字符串 → 检测项通过
- 命令执行成功但输出不包含期望字符串 → 检测项失败
- 命令执行失败 → 检测项失败
- 期望字符串为空时，向后兼容：只检查命令执行是否成功

## 涉及检测项类型

| 检测类型 | 是否支持输出检查 | 说明 |
|---------|----------------|------|
| REMOTE_COMMAND | 是 | 检查 stdout |
| LOCAL_SCRIPT | 是 | 检查 stdout |
| REMOTE_SCRIPT | 是 | 检查 stdout |
| HTTP_REQUEST | 是 | 检查 response body |
| PORT_PROBE | 否 | 只有连通性，无输出内容 |

## 设计

### 1. 模型层

**文件**: `src/main/java/io/github/yueryou/easydev/plugin/model/DelayCheckItem.java`

新增字段：
```java
private String expectedOutput;
```

新增 getter/setter：
```java
public String getExpectedOutput() {
    return expectedOutput;
}

public void setExpectedOutput(String expectedOutput) {
    this.expectedOutput = expectedOutput;
}
```

### 2. 执行器层

**文件**: `src/main/java/io/github/yueryou/easydev/plugin/executor/DelayCheckExecutor.java`

修改以下方法，对输出内容进行检查：
- `executeRemoteCommand(DelayCheckItem item, ExecutionContext context)`
- `executeLocalScript(DelayCheckItem item, ExecutionContext context)`
- `executeRemoteScript(DelayCheckItem item, ExecutionContext context)`
- `executeHttpRequest(DelayCheckItem item, ExecutionContext context)`

新增私有方法：
```java
private static boolean outputContainsExpected(String output, String expected) {
    if (expected == null || expected.isEmpty()) {
        return true;
    }
    if (output == null) {
        return false;
    }
    return output.toLowerCase().contains(expected.toLowerCase());
}
```

各检测方法修改后的逻辑：

```
执行命令
├─ 命令执行失败（exit code ≠ 0）→ FAIL
└─ 命令执行成功（exit code = 0）
    ├─ expectedOutput 为空 → 成功（向后兼容）
    └─ expectedOutput 不为空
        ├─ 输出包含 expectedOutput（不区分大小写）→ 成功
        └─ 输出不包含 expectedOutput → FAIL，错误消息包含期望字符串
```

### 3. UI 层

**文件**: `src/main/java/io/github/yueryou/easydev/plugin/ui/dialog/CheckItemEditDialog.java`

新增字段：
```java
private JTextField expectedOutputField;
```

新增方法：
```java
private JPanel createExpectedOutputPanel() {
    expectedOutputField = new JTextField(30);
    JLabel tipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
    return FormBuilder.createFormBuilder()
            .addLabeledComponent("期望输出包含", expectedOutputField)
            .addComponent(tipLabel)
            .getPanel();
}
```

在 `createCenterPanel()` 中：
- REMOTE_COMMAND、REMOTE_SCRIPT、LOCAL_SCRIPT、HTTP_REQUEST 的配置面板中新增 `createExpectedOutputPanel()`
- PORT_PROBE 的配置面板中不显示该字段

在 `populateFieldsFromItem()` 中填充值。

在 `createItemFromFields()` 中保存值。

### 4. 序列化层

**文件**: `src/main/java/io/github/yueryou/easydev/plugin/model/PipelineStepWrapper.java`

`serializeItem()` 方法中新增：
```java
"::" + escape(item.getExpectedOutput()) +
```

`deserializeItem()` 方法中新增：
```java
item.setExpectedOutput(unescape(parts[12]));
```

字段数组索引从 12 调整为 13。

### 5. 国际化

**文件**: `src/main/resources/messages_zh.properties`
**文件**: `src/main/resources/messages_en.properties`

如需新增国际化 key（可选，当前使用中文字段名）。

## 实现顺序

1. `DelayCheckItem.java` - 新增 expectedOutput 字段
2. `PipelineStepWrapper.java` - 序列化/反序列化支持
3. `DelayCheckExecutor.java` - 执行器输出检查逻辑
4. `CheckItemEditDialog.java` - UI 配置支持
