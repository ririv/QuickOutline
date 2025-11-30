package com.ririv.quickoutline.view.event;

import com.ririv.quickoutline.pdfProcess.ViewScaleType;

public class ViewScaleChangedEvent {
    public final ViewScaleType viewScaleType;

    public ViewScaleChangedEvent(ViewScaleType viewScaleType) {
        this.viewScaleType = viewScaleType;
    }
}