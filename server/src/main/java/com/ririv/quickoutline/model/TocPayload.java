package com.ririv.quickoutline.model;

public record TocPayload(String tocContent, String title, int offset, int insertPos, String style, SectionConfig header, SectionConfig footer) {}
