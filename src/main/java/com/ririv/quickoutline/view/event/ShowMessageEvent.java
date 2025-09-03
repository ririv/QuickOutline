package com.ririv.quickoutline.view.event;

import com.ririv.quickoutline.view.controls.message.Message;

public class ShowMessageEvent {
    public final String message;
    public final Message.MessageType messageType;

    public ShowMessageEvent(String message, Message.MessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }
}
