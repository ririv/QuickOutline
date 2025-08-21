package com.ririv.quickoutline.event;

import com.ririv.quickoutline.pdfProcess.ViewScaleType;

public class SetContentsEvent {
    public final ViewScaleType viewScaleType;

    public SetContentsEvent(ViewScaleType viewScaleType) {
        this.viewScaleType = viewScaleType;
    }
}
