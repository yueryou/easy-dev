# 延迟检查输出内容检查实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为延迟检查任务的检测项增加输出内容检查能力，命令执行成功后检查输出是否包含期望字符串（不区分大小写）。

**Architecture:** 在 `DelayCheckItem` 新增 `expectedOutput` 字段，在各检测类型执行器中增加输出内容匹配逻辑，UI 层支持配置该字段，序列化层支持持久化。

**Tech Stack:** Java, Swing (IntelliJ UI), IntelliJ Platform Plugin

---

## 文件变更概览

| 文件 | 变更类型 | 职责 |
|------|---------|------|
| `DelayCheckItem.java` | 修改 | 新增 expectedOutput 字段及 getter/setter |
| `PipelineStepWrapper.java` | 修改 | 序列化/反序列化支持 expectedOutput |
| `DelayCheckExecutor.java` | 修改 | 各检测方法增加输出内容检查逻辑 |
| `CheckItemEditDialog.java` | 修改 | UI 支持配置 expectedOutput 字段 |

---

## Task 1: DelayCheckItem 新增 expectedOutput 字段

**文件:**
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/model/DelayCheckItem.java:29`

- [ ] **Step 1: 添加字段**

在 `DelayCheckItem.java` 中，`timeout` 字段下方添加：

```java
/**
 * 期望输出包含的字符串（不区分大小写），为空时不检查输出内容
 */
private String expectedOutput;
```

- [ ] **Step 2: 添加 getter/setter**

在 `getTimeout()` 方法后添加：

```java
public String getExpectedOutput() {
    return expectedOutput;
}

public void setExpectedOutput(String expectedOutput) {
    this.expectedOutput = expectedOutput;
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/io/github/yueryou/easydev/plugin/model/DelayCheckItem.java
git commit -m "feat(delay-check): add expectedOutput field to DelayCheckItem

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: PipelineStepWrapper 序列化支持

**文件:**
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/model/PipelineStepWrapper.java:152-165` (serializeItem)
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/model/PipelineStepWrapper.java:183-207` (deserializeItem)

- [ ] **Step 1: 修改 serializeItem 方法**

在 `serializeItem()` 方法的末尾，在 `item.getTimeout()` 之后添加：

```java
"::" + escape(item.getExpectedOutput());
```

注意：`item.getTimeout()` 后面原来没有 `+`，需要补上。

- [ ] **Step 2: 修改 deserializeItem 方法**

在反序列化部分，`parts` 数组长度检查从 `parts.length < 12` 改为 `parts.length < 13`。

在设置 `timeout` 之后添加：

```java
item.setExpectedOutput(unescape(parts[12]));
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/io/github/yueryou/easydev/plugin/model/PipelineStepWrapper.java
git commit -m "feat(delay-check): support expectedOutput serialization in PipelineStepWrapper

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: DelayCheckExecutor 执行器输出检查逻辑

**文件:**
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/executor/DelayCheckExecutor.java:140-174` (executeRemoteCommand)
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/executor/DelayCheckExecutor.java:179-215` (executeLocalScript)
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/executor/DelayCheckExecutor.java:220-257` (executeRemoteScript)
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/executor/DelayCheckExecutor.java:262-310` (executeHttpRequest)

- [ ] **Step 1: 新增 outputContainsExpected 工具方法**

在 `DelayCheckExecutor.java` 的 Utility Methods 区域（约第 341 行附近），`createScriptProcessBuilder` 方法之前添加：

```java
/**
 * 检查输出是否包含期望字符串（不区分大小写）
 * @param output 命令输出
 * @param expected 期望包含的字符串，为空则返回 true
 * @return true 如果输出包含期望字符串，或期望字符串为空
 */
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

- [ ] **Step 2: 修改 executeRemoteCommand 方法**

将方法末尾的逻辑从：
```java
if (status.isSuccess()) {
    return StepResult.success(status.getMessage(), 0);
} else {
    StepResult result = StepResult.failure("远程命令执行失败: " + status.getMessage());
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

改为：
```java
if (status.isSuccess()) {
    String output = status.getMessage();
    if (outputContainsExpected(output, item.getExpectedOutput())) {
        return StepResult.success(output, 0);
    } else {
        StepResult result = StepResult.failure("输出不包含期望字符串: " + item.getExpectedOutput());
        result.setStdout(output);
        result.setDuration(Duration.ofMillis(duration));
        return result;
    }
} else {
    StepResult result = StepResult.failure("远程命令执行失败: " + status.getMessage());
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

- [ ] **Step 3: 修改 executeLocalScript 方法**

在读取 stdout 后（`String output = readStream(process.getInputStream());`），检查之前添加：

```java
// 检查输出内容
if (exitCode == 0 && !outputContainsExpected(output, item.getExpectedOutput())) {
    StepResult result = StepResult.failure("输出不包含期望字符串: " + item.getExpectedOutput());
    result.setStdout(output);
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

然后将原来的 `if (exitCode == 0)` 分支改为直接成功（因为前面的检查已覆盖）：

```java
if (exitCode == 0) {
    StepResult result = StepResult.success(output, exitCode);
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

等等 — 需要重新组织逻辑。修改后的完整逻辑应该是：

```java
String output = readStream(process.getInputStream());
int exitCode = process.exitValue();

if (exitCode != 0) {
    StepResult result = StepResult.failure("本地脚本执行失败，退出码: " + exitCode);
    result.setStdout(output);
    result.setDuration(Duration.ofMillis(duration));
    return result;
}

// exitCode == 0，检查输出内容
if (!outputContainsExpected(output, item.getExpectedOutput())) {
    StepResult result = StepResult.failure("输出不包含期望字符串: " + item.getExpectedOutput());
    result.setStdout(output);
    result.setDuration(Duration.ofMillis(duration));
    return result;
}

StepResult result = StepResult.success(output, exitCode);
result.setDuration(Duration.ofMillis(duration));
return result;
```

- [ ] **Step 4: 修改 executeRemoteScript 方法**

与 `executeRemoteCommand` 类似，将成功分支改为：

```java
if (status.isSuccess()) {
    String output = status.getMessage();
    if (outputContainsExpected(output, item.getExpectedOutput())) {
        return StepResult.success(output, 0);
    } else {
        StepResult result = StepResult.failure("输出不包含期望字符串: " + item.getExpectedOutput());
        result.setStdout(output);
        result.setDuration(Duration.ofMillis(duration));
        return result;
    }
} else {
    StepResult result = StepResult.failure("远程脚本执行失败: " + status.getMessage());
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

- [ ] **Step 5: 修改 executeHttpRequest 方法**

在获取 response body 后，检查状态码成功后，增加输出内容检查：

将原来的：
```java
if (statusCode == expectedCode) {
    String body = response.body();
    if (body.length() > 500) {
        body = body.substring(0, 500) + "...";
    }
    StepResult result = StepResult.success("HTTP " + statusCode + " - " + body, 0);
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

改为：
```java
if (statusCode == expectedCode) {
    String body = response.body();
    if (body.length() > 500) {
        body = body.substring(0, 500) + "...";
    }
    if (!outputContainsExpected(body, item.getExpectedOutput())) {
        StepResult result = StepResult.failure("响应内容不包含期望字符串: " + item.getExpectedOutput());
        result.setStdout(body);
        result.setDuration(Duration.ofMillis(duration));
        return result;
    }
    StepResult result = StepResult.success("HTTP " + statusCode + " - " + body, 0);
    result.setDuration(Duration.ofMillis(duration));
    return result;
}
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/io/github/yueryou/easydev/plugin/executor/DelayCheckExecutor.java
git commit -m "feat(delay-check): add output content check in DelayCheckExecutor

Support case-insensitive output matching for remote command, local/remote script and HTTP request check types.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: CheckItemEditDialog UI 支持

**文件:**
- Modify: `src/main/java/io/github/yueryou/easydev/plugin/ui/dialog/CheckItemEditDialog.java`

需要修改的位置：
1. 新增 `expectedOutputField` 字段声明
2. 在各检测类型的配置面板中新增期望输出字段
3. `populateFieldsFromItem()` 中填充值
4. `createItemFromFields()` 中保存值

- [ ] **Step 1: 添加字段声明**

在 `timeoutField` 字段声明后添加：

```java
private JTextField expectedOutputField;
```

- [ ] **Step 2: 修改 createRemoteCommandPanel**

将 `createRemoteCommandPanel()` 改为：

```java
private JPanel createRemoteCommandPanel() {
    expectedOutputField = new JTextField(30);
    JLabel tipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
    return FormBuilder.createFormBuilder()
            .addLabeledComponent("服务器", remoteServerComboBox)
            .addLabeledComponent("命令", remoteCommandField)
            .addLabeledComponent("期望输出包含", expectedOutputField)
            .addComponent(tipLabel)
            .getPanel();
}
```

注意：原来的 `createRemoteCommandPanel()` 不包含 `expectedOutputField`，需要重构。

- [ ] **Step 3: 修改 createLocalScriptPanel**

```java
private JPanel createLocalScriptPanel() {
    JLabel tipLabel = new JLabel("<html><font color='#808080'>支持 .sh/.bash/.bat/.cmd/.ps1/.py，自动识别解释器</font></html>");
    JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
    return FormBuilder.createFormBuilder()
            .addLabeledComponent("脚本路径", scriptPathField)
            .addLabeledComponent("期望输出包含", expectedOutputField)
            .addComponent(tipLabel)
            .addComponent(outputTipLabel)
            .getPanel();
}
```

- [ ] **Step 4: 修改 createRemoteScriptPanel**

```java
private JPanel createRemoteScriptPanel() {
    JLabel tipLabel = new JLabel("<html><font color='#808080'>通过 bash 执行远程脚本</font></html>");
    JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查输出内容</font></html>");
    return FormBuilder.createFormBuilder()
            .addLabeledComponent("服务器", scriptServerComboBox)
            .addLabeledComponent("远程脚本路径", scriptPathField)
            .addLabeledComponent("期望输出包含", expectedOutputField)
            .addComponent(tipLabel)
            .addComponent(outputTipLabel)
            .getPanel();
}
```

- [ ] **Step 5: 修改 createHttpRequestPanel**

```java
private JPanel createHttpRequestPanel() {
    JLabel outputTipLabel = new JLabel("<html><font color='#808080'>不区分大小写，为空则不检查响应内容</font></html>");
    return FormBuilder.createFormBuilder()
            .addLabeledComponent("URL", httpUrlField)
            .addLabeledComponent("方法", httpMethodComboBox)
            .addLabeledComponent("请求体 (POST)", httpBodyField)
            .addLabeledComponent("期望状态码", httpExpectedCodeField)
            .addLabeledComponent("期望输出包含", expectedOutputField)
            .addComponent(outputTipLabel)
            .getPanel();
}
```

- [ ] **Step 6: 修改 populateFieldsFromItem 方法**

在 `switch (item.getType())` 中为 REMOTE_COMMAND、LOCAL_SCRIPT、REMOTE_SCRIPT、HTTP_REQUEST 添加：

```java
case REMOTE_COMMAND:
    remoteCommandField.setText(item.getCommand());
    selectItemInComboBox(remoteServerComboBox, item.getServerId());
    expectedOutputField.setText(item.getExpectedOutput());
    break;
case LOCAL_SCRIPT:
    scriptPathField.setText(item.getScriptPath());
    expectedOutputField.setText(item.getExpectedOutput());
    break;
case REMOTE_SCRIPT:
    scriptPathField.setText(item.getScriptPath());
    selectItemInComboBox(scriptServerComboBox, item.getServerId());
    expectedOutputField.setText(item.getExpectedOutput());
    break;
```

HTTP_REQUEST 的 case 中也需要添加：
```java
case HTTP_REQUEST:
    httpUrlField.setText(item.getUrl());
    httpMethodComboBox.setSelectedItem(item.getHttpMethod());
    httpBodyField.setText(item.getHttpBody());
    httpExpectedCodeField.setText(String.valueOf(item.getExpectedStatusCode()));
    expectedOutputField.setText(item.getExpectedOutput());
    break;
```

- [ ] **Step 7: 修改 createItemFromFields 方法**

在各个 case 分支中，从 `expectedOutputField` 获取值并设置到 item：

```java
case REMOTE_COMMAND:
    String cmd = remoteCommandField.getText().trim();
    if (cmd.isEmpty()) {
        showError("请输入远程命令");
        return null;
    }
    item.setCommand(cmd);
    item.setServerId(extractIdFromComboBoxItem((String) remoteServerComboBox.getSelectedItem()));
    item.setExpectedOutput(expectedOutputField.getText());
    break;
```

LOCAL_SCRIPT、REMOTE_SCRIPT、HTTP_REQUEST 同样添加 `item.setExpectedOutput(expectedOutputField.getText());`。

- [ ] **Step 8: 提交**

```bash
git add src/main/java/io/github/yueryou/easydev/plugin/ui/dialog/CheckItemEditDialog.java
git commit -m "feat(delay-check): add expectedOutput field to CheckItemEditDialog UI

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 整体验证

- [ ] **Step 1: 构建验证**

```bash
./gradlew buildPlugin
```

确保无编译错误。

- [ ] **Step 2: 提交所有变更**

如之前各 Task 的提交已包含所有变更，此步可跳过。若有遗漏，执行：

```bash
git add -A
git commit -m "feat: implement delay check output content validation

- Add expectedOutput field to DelayCheckItem
- Support serialization in PipelineStepWrapper
- Add output content check in DelayCheckExecutor
- Add UI support in CheckItemEditDialog

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 完成后检查清单

- [ ] DelayCheckItem 新增 `expectedOutput` 字段
- [ ] PipelineStepWrapper 序列化/反序列化正确处理 `expectedOutput`
- [ ] DelayCheckExecutor 四种检测类型均支持输出内容检查（不区分大小写）
- [ ] CheckItemEditDialog UI 正确显示/保存 `expectedOutput` 字段
- [ ] PORT_PROBE 类型不显示期望输出字段
- [ ] `expectedOutput` 为空时向后兼容（只检查命令是否成功）
- [ ] 构建通过
