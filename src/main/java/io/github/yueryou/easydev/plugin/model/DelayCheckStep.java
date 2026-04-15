package io.github.yueryou.easydev.plugin.model;

import com.intellij.util.xmlb.annotations.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * 延迟检查步骤
 *
 * 在设定的时间内，周期性执行后台检测任务。
 * 所有检测项全部成功才认为本步骤成功。
 */
@Tag("delay-check-step")
public class DelayCheckStep extends PipelineStep {

    /**
     * 总检测时长（秒）
     */
    private int duration = 60;

    /**
     * 检测间隔（秒）
     */
    private int interval = 5;

    /**
     * 检测子项列表
     */
    private List<DelayCheckItem> checkItems = new ArrayList<>();

    public DelayCheckStep() {
        this.type = StepType.DELAY_CHECK;
    }

    public DelayCheckStep(String name) {
        super(name, StepType.DELAY_CHECK);
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public List<DelayCheckItem> getCheckItems() {
        return checkItems;
    }

    public void setCheckItems(List<DelayCheckItem> checkItems) {
        this.checkItems = checkItems;
    }
}
