package com.ririv.quickoutline.event;

import com.ririv.quickoutline.view.MainController;

public class SwitchTabEvent {
    public final MainController.FnTab targetTab;

    public  SwitchTabEvent(MainController.FnTab targetTab) {
        this.targetTab = targetTab;
    }
}
