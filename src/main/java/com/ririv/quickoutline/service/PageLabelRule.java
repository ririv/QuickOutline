
package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.PageLabel;

/**
 * Represents a user-defined rule for page labeling.
 * This is a data-transfer object used between the UI layer and the service layer.
 */
public record PageLabelRule(
        int fromPage,
        PageLabel.PageLabelNumberingStyle style,
        String prefix,
        int start
) {}
