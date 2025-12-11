package com.ririv.quickoutline.api.model;

import com.ririv.quickoutline.model.SectionConfig;
import com.ririv.quickoutline.model.PageLabel;
import java.util.List;

public record TocConfig(
    String tocContent,
    String title,
    int insertPos,
    PageLabel.PageLabelNumberingStyle numberingStyle,
    SectionConfig header,
    SectionConfig footer,
    List<TocLinkDto> links
) {}
