package com.ririv.quickoutline.event;

import com.ririv.quickoutline.pdfProcess.ViewScaleType;

public class SetContentsEvent {
    private final ViewScaleType viewScaleType;

    public SetContentsEvent(ViewScaleType viewScaleType) {
        this.viewScaleType = viewScaleType;
    }

    public ViewScaleType getViewScaleType() {
        return viewScaleType;
    }
}
