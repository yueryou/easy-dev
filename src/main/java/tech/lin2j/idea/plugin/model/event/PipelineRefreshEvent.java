package tech.lin2j.idea.plugin.model.event;

import tech.lin2j.idea.plugin.event.ApplicationEvent;

/**
 * Event triggered to refresh pipeline list UI
 *
 * @author Easy Deploy Plugin
 * @date 2024/12/XX XX:XX
 */
public class PipelineRefreshEvent extends ApplicationEvent {

    public PipelineRefreshEvent() {
        super(new Object());
    }
}