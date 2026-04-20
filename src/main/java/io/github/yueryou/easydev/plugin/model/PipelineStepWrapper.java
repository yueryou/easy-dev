package io.github.yueryou.easydev.plugin.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Tag;
import io.github.yueryou.easydev.plugin.log.UnifiedLogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PipelineStep 的包装类，用于 XmlSerializer 序列化多态类型。
 *
 * 由于 IntelliJ 的 XmlSerializer 不支持多态类型的直接序列化，
 * 需要将不同类型的步骤（LocalCommandStep, UploadStep, RemoteCommandStep, DelayCheckStep）
 * 转换为扁平结构存储，反序列化时再根据 type 字段重建原始类型。
 */
@Tag("pipeline-step-wrapper")
public class PipelineStepWrapper {

    private static final Logger LOG = Logger.getInstance(PipelineStepWrapper.class);

    private StepType type;
    private String uid;
    private String name;
    private boolean enabled;

    // LocalCommandStep 字段
    private String command;
    private String workingDir;
    private int timeout;
    private String commandId;

    // UploadStep 字段
    private String uploadProfileId;
    private String serverId;
    private boolean createRemoteDir;

    // RemoteCommandStep 字段
    private String remoteCommand;
    private String remoteWorkingDir;
    private String remoteCommandId;
    private String remoteServerId;

    // DelayCheckStep 字段
    private int delayCheckDuration;
    private int delayCheckInterval;
    /**
     * JSON 序列化后的检测项列表
     */
    private String delayCheckItemsJson;

    public PipelineStepWrapper() {
    }

    public PipelineStepWrapper(PipelineStep step) {
        // 防御性处理 null 输入，避免反序列化时 NPE
        if (step == null) {
            this.type = null;
            this.uid = null;
            this.name = null;
            this.enabled = true;
            LOG.info("[PipelineStepWrapper] Constructor received NULL step");
            return;
        }

        this.type = step.getType();
        this.uid = step.getUid();
        this.name = step.getName();
        this.enabled = step.isEnabled();

        LOG.info("[PipelineStepWrapper] Wrapping step: type=" + type + ", uid=" + uid + ", name=" + name + ", enabled=" + enabled);

        if (step instanceof LocalCommandStep) {
            LocalCommandStep localStep = (LocalCommandStep) step;
            this.command = localStep.getCommand();
            this.workingDir = localStep.getWorkingDir();
            this.timeout = localStep.getTimeout();
            this.commandId = localStep.getCommandId();
        } else if (step instanceof UploadStep) {
            UploadStep uploadStep = (UploadStep) step;
            this.uploadProfileId = uploadStep.getUploadProfileId();
            this.serverId = uploadStep.getServerId();
            this.createRemoteDir = uploadStep.isCreateRemoteDir();
        } else if (step instanceof RemoteCommandStep) {
            RemoteCommandStep remoteStep = (RemoteCommandStep) step;
            this.remoteCommand = remoteStep.getCommand();
            this.remoteWorkingDir = remoteStep.getWorkingDir();
            this.remoteCommandId = remoteStep.getCommandId();
            this.remoteServerId = remoteStep.getServerId();
        } else if (step instanceof DelayCheckStep) {
            DelayCheckStep checkStep = (DelayCheckStep) step;
            this.delayCheckDuration = checkStep.getDuration();
            this.delayCheckInterval = checkStep.getInterval();
            this.delayCheckItemsJson = serializeCheckItems(checkStep.getCheckItems());
            LOG.info("[PipelineStepWrapper] Serialized DelayCheckStep items: count=" + checkStep.getCheckItems().size() + ", json=" + this.delayCheckItemsJson);
        }
    }

    /**
     * 将包装对象转换为具体 PipelineStep 实例。
     */
    public PipelineStep toStep() {
        LOG.info("[PipelineStepWrapper] toStep() called: type=" + type + ", uid=" + uid + ", name=" + name);

        if (type == null) {
            LOG.info("[PipelineStepWrapper] toStep() returning NULL - type is null");
            return null;
        }

        PipelineStep step;
        switch (type) {
            case LOCAL_COMMAND:
                LocalCommandStep localStep = new LocalCommandStep();
                localStep.setCommand(command);
                localStep.setWorkingDir(workingDir);
                localStep.setTimeout(timeout);
                localStep.setCommandId(commandId);
                step = localStep;
                LOG.info("[PipelineStepWrapper] toStep() created LocalCommandStep: " + localStep.getName());
                break;
            case UPLOAD:
                UploadStep uploadStep = new UploadStep();
                uploadStep.setUploadProfileId(uploadProfileId);
                uploadStep.setServerId(serverId);
                uploadStep.setCreateRemoteDir(createRemoteDir);
                step = uploadStep;
                LOG.info("[DEBUG] toStep() created UploadStep: " + uploadStep.getUploadProfileId());
                break;
            case REMOTE_COMMAND:
                RemoteCommandStep remoteStep = new RemoteCommandStep();
                remoteStep.setCommand(remoteCommand);
                remoteStep.setWorkingDir(remoteWorkingDir);
                remoteStep.setCommandId(remoteCommandId);
                remoteStep.setServerId(remoteServerId);
                step = remoteStep;
                LOG.info("[DEBUG] toStep() created RemoteCommandStep: " + remoteStep.getCommand());
                break;
            case DELAY_CHECK:
                DelayCheckStep checkStep = new DelayCheckStep();
                checkStep.setDuration(delayCheckDuration);
                checkStep.setInterval(delayCheckInterval);
                LOG.info("[PipelineStepWrapper] Deserializing DelayCheckStep: delayCheckItemsJson=" + delayCheckItemsJson);
                checkStep.setCheckItems(deserializeCheckItems(delayCheckItemsJson));
                step = checkStep;
                LOG.info("[PipelineStepWrapper] toStep() created DelayCheckStep: " + checkStep.getName() + ", duration=" + checkStep.getDuration() + ", items=" + checkStep.getCheckItems().size());
                break;
            default:
                LOG.info("[PipelineStepWrapper] toStep() returning NULL - unknown type");
                return null;
        }

        step.setUid(uid);
        step.setName(name);
        step.setEnabled(enabled);
        LOG.info("[PipelineStepWrapper] toStep() returning step: " + step.getClass().getSimpleName() + ", name=" + step.getName() + ", type=" + step.getType());
        return step;
    }

    // ==================== Serialization Helpers ====================

    private static String serializeCheckItems(List<DelayCheckItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append("|||");
            sb.append(serializeItem(items.get(i)));
        }
        return sb.toString();
    }

    private static String serializeItem(DelayCheckItem item) {
        return item.getType().name() +
                "::" + escape(item.getName()) +
                "::" + escape(item.getServerId()) +
                "::" + escape(item.getCommand()) +
                "::" + escape(item.getScriptPath()) +
                "::" + escape(item.getUrl()) +
                "::" + escape(item.getHttpMethod()) +
                "::" + escape(item.getHttpBody()) +
                "::" + item.getExpectedStatusCode() +
                "::" + escape(item.getHost()) +
                "::" + item.getPort() +
                "::" + item.getTimeout() +
                "::" + escape(item.getExpectedOutput());
    }

    @Nullable
    private static List<DelayCheckItem> deserializeCheckItems(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        List<DelayCheckItem> items = new ArrayList<>();
        String[] parts = json.split("\\|\\|\\|");
        for (String part : parts) {
            DelayCheckItem item = deserializeItem(part);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    @Nullable
    private static DelayCheckItem deserializeItem(String data) {
        try {
            String[] parts = data.split("::", -1);
            if (parts.length < 13) {
                return null;
            }
            DelayCheckItem item = new DelayCheckItem();
            item.setType(CheckItemType.valueOf(parts[0]));
            item.setName(unescape(parts[1]));
            item.setServerId(unescape(parts[2]));
            item.setCommand(unescape(parts[3]));
            item.setScriptPath(unescape(parts[4]));
            item.setUrl(unescape(parts[5]));
            item.setHttpMethod(unescape(parts[6]));
            item.setHttpBody(unescape(parts[7]));
            item.setExpectedStatusCode(Integer.parseInt(parts[8]));
            item.setHost(unescape(parts[9]));
            item.setPort(Integer.parseInt(parts[10]));
            item.setTimeout(Integer.parseInt(parts[11]));
            item.setExpectedOutput(unescape(parts[12]));
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(":", "\\:").replace("|", "\\|");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == '\\' || next == ':' || next == '|') {
                    sb.append(next);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // ==================== Getters & Setters ====================

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getUploadProfileId() {
        return uploadProfileId;
    }

    public void setUploadProfileId(String uploadProfileId) {
        this.uploadProfileId = uploadProfileId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public boolean isCreateRemoteDir() {
        return createRemoteDir;
    }

    public void setCreateRemoteDir(boolean createRemoteDir) {
        this.createRemoteDir = createRemoteDir;
    }

    public String getRemoteCommand() {
        return remoteCommand;
    }

    public void setRemoteCommand(String remoteCommand) {
        this.remoteCommand = remoteCommand;
    }

    public String getRemoteWorkingDir() {
        return remoteWorkingDir;
    }

    public void setRemoteWorkingDir(String remoteWorkingDir) {
        this.remoteWorkingDir = remoteWorkingDir;
    }

    public String getRemoteCommandId() {
        return remoteCommandId;
    }

    public void setRemoteCommandId(String remoteCommandId) {
        this.remoteCommandId = remoteCommandId;
    }

    public String getRemoteServerId() {
        return remoteServerId;
    }

    public void setRemoteServerId(String remoteServerId) {
        this.remoteServerId = remoteServerId;
    }

    public int getDelayCheckDuration() {
        return delayCheckDuration;
    }

    public void setDelayCheckDuration(int delayCheckDuration) {
        this.delayCheckDuration = delayCheckDuration;
    }

    public int getDelayCheckInterval() {
        return delayCheckInterval;
    }

    public void setDelayCheckInterval(int delayCheckInterval) {
        this.delayCheckInterval = delayCheckInterval;
    }

    public String getDelayCheckItemsJson() {
        return delayCheckItemsJson;
    }

    public void setDelayCheckItemsJson(String delayCheckItemsJson) {
        this.delayCheckItemsJson = delayCheckItemsJson;
    }
}
