package com.ririv.quickoutline.service;

import java.util.Objects;

/**
 * Represents a download-related event (e.g. fonts or other assets).
 * This is intentionally UI-agnostic; higher layers can translate it into
 * localized user-facing messages.
 *
 * @param type         The state of the download
 * @param resourceName The name of the file/resource
 * @param detail       Extra info (e.g. error message), can be null
 */
public record DownloadEvent(Type type, String resourceName, String detail) {

    public enum Type {
        START,
        PROGRESS,
        SUCCESS,
        ERROR
    }

    /**
     * Compact constructor for validation.
     */
    public DownloadEvent {
        Objects.requireNonNull(type, "type cannot be null");
    }
}