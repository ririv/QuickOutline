package com.ririv.quickoutline.api.model;

import com.ririv.quickoutline.model.SectionConfig;
import com.ririv.quickoutline.model.PageLabel;

public record TocConfig(
    String tocContent,
    String title,
    int insertPos,
    PageLabel.PageLabelNumberingStyle style,
    SectionConfig header,
    SectionConfig footer
) {}
