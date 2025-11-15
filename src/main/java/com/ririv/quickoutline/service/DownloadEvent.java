package com.ririv.quickoutline.service;

import java.util.Objects;

/**
 * Represents a download-related event (e.g. fonts or other assets).
 * This is intentionally UI-agnostic; higher layers can translate it into
 * localized user-facing messages.
 */
public final class DownloadEvent {

    public enum Type {
        START,
        PROGRESS,
        SUCCESS,
        ERROR
    }

    private final Type type;
    private final String resourceName;
    private final String detail;

    public DownloadEvent(Type type, String resourceName, String detail) {
        this.type = Objects.requireNonNull(type, "type");
        this.resourceName = resourceName;
        this.detail = detail;
    }

    public Type getType() {
        return type;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getDetail() {
        return detail;
    }
}
