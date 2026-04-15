package io.github.yueryou.easydev.plugin.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Tag;
import io.github.yueryou.easydev.plugin.log.UnifiedLogger;
import tech.lin2j.idea.plugin.model.UniqueModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流水线配置类
 *
 * 注意：steps 字段存储多态类型（LocalCommandStep, UploadStep, RemoteCommandStep）
 * 使用 PipelineStepWrapper 进行 XmlSerializer 序列化
 */
public class Pipeline implements UniqueModel {

    private static final Logger LOG = Logger.getInstance(Pipeline.class);

    private String id;
    private String uid;
    private String name;

    // 存储步骤的包装列表，用于 XmlSerializer 序列化
    @Tag("step")
    private List<PipelineStepWrapper> steps;
    private FailureStrategy onFailure;
    private long createdAt;
    private long updatedAt;

    // 缓存已转换的步骤列表，避免重复创建对象
    private transient List<PipelineStep> cachedPipelineSteps;

    public Pipeline() {
        this.steps = new ArrayList<>();
        this.onFailure = FailureStrategy.STOP;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取步骤列表（供 XmlSerializer 序列化/反序列化使用）。
     */
    public List<PipelineStepWrapper> getSteps() {
        return steps;
    }

    /**
     * 设置步骤列表（供 XmlSerializer 序列化/反序列化使用）。
     */
    public void setSteps(List<PipelineStepWrapper> steps) {
        UnifiedLogger.getInstance().debug("Pipeline", String.format(
            "setSteps() called: name=%s, steps=%s",
            name, steps != null ? steps.size() : "null"));
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                PipelineStepWrapper w = steps.get(i);
                UnifiedLogger.getInstance().debug("Pipeline", String.format(
                    "  step[%d]: type=%s, uid=%s, name=%s",
                    i, w != null ? w.getType() : "null", w != null ? w.getUid() : "null", w != null ? w.getName() : "null"));
            }
        }
        this.steps = steps;
    }

    /**
     * 获取转换后的 PipelineStep 列表（供业务逻辑使用）。
     */
    public List<PipelineStep> getPipelineSteps() {
        LOG.info("[Pipeline] getPipelineSteps() called: name=" + name + ", uid=" + uid +
            ", steps.field=" + (steps != null ? steps.size() : "NULL"));

        if (steps == null) {
            LOG.info("[Pipeline] steps field is NULL!");
            return new ArrayList<>();
        } else if (steps.isEmpty()) {
            LOG.info("[Pipeline] steps field is EMPTY!");
            return new ArrayList<>();
        }

        LOG.info("[Pipeline] Converting " + steps.size() + " wrappers to steps");
        for (int i = 0; i < steps.size(); i++) {
            PipelineStepWrapper w = steps.get(i);
            LOG.info("[Pipeline]   wrapper[" + i + "]: type=" +
                (w != null ? w.getType() : "NULL") +
                ", uid=" + (w != null ? w.getUid() : "NULL"));
        }

        List<PipelineStep> result = steps.stream()
                .filter(wrapper -> wrapper != null)
                .map(wrapper -> {
                    PipelineStep step = wrapper.toStep();
                    LOG.info("[Pipeline]   toStep() returned: " +
                        (step != null ? step.getClass().getSimpleName() + ", name=" + step.getName() : "NULL"));
                    return step;
                })
                .filter(step -> step != null)
                .collect(Collectors.toList());

        LOG.info("[Pipeline] Converted to " + result.size() + " valid steps");
        return result;
    }

    /**
     * 设置 PipelineStep 列表（供业务逻辑使用）。
     * 会自动转换为 PipelineStepWrapper 存储。
     */
    public void setPipelineSteps(List<PipelineStep> pipelineSteps) {
        this.steps = new ArrayList<>();
        if (pipelineSteps != null) {
            for (PipelineStep step : pipelineSteps) {
                // 跳过 null 元素，避免反序列化问题
                if (step != null) {
                    steps.add(new PipelineStepWrapper(step));
                }
            }
        }
    }

    public FailureStrategy getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(FailureStrategy onFailure) {
        this.onFailure = onFailure;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pipeline pipeline = (Pipeline) o;
        if (id != null && pipeline.id != null) {
            return Objects.equals(id, pipeline.id);
        }
        return Objects.equals(uid, pipeline.uid);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(uid);
    }
}
