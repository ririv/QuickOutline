package com.ririv.quickoutline.view.event;

import com.ririv.quickoutline.view.MainController;

public class SwitchTabEvent {
    public final MainController.FnTab targetTab;

    public  SwitchTabEvent(MainController.FnTab targetTab) {
        this.targetTab = targetTab;
    }
}
