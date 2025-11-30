package com.ririv.quickoutline.view.event;

public class FontDownloadEvent {
    public enum Status {
        STARTED,
        FINISHED,
        FAILED
    }

    private final Status status;
    private final String message;

    public FontDownloadEvent(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
