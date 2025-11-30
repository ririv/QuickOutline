package com.ririv.quickoutline.model;

public record SectionConfig(String left, String center, String right, String inner, String outer, boolean drawLine) {
    public boolean hasContent() {
        return (left != null && !left.isBlank()) ||
               (center != null && !center.isBlank()) ||
               (right != null && !right.isBlank()) ||
               (inner != null && !inner.isBlank()) ||
               (outer != null && !outer.isBlank());
    }
}
