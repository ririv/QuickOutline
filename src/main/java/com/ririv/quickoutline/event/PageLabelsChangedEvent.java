package com.ririv.quickoutline.event;

import java.util.List;

public class PageLabelsChangedEvent {
    private final List<String> pageLabels;

    public PageLabelsChangedEvent(List<String> pageLabels) {
        this.pageLabels = pageLabels;
    }

    public List<String> getPageLabels() {
        return pageLabels;
    }
}
