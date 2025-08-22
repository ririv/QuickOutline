package com.ririv.quickoutline.event;

import com.ririv.quickoutline.view.controls.Message;

public class ShowMessageEvent {
    public final String message;
    public final Message.MessageType messageType;

    public ShowMessageEvent(String message, Message.MessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }
}
