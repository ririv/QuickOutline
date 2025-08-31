package com.ririv.quickoutline.event;

public class ShowMessageEvent {
    public final String message;
    public final String messageType;

    public ShowMessageEvent(String message, String messageType) {
        this.message = message;
        this.messageType = messageType;
    }
}
