package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import java.util.Map;

public class DocumentStats {
    public final Style dominantTextStyle;
    public final double medianLineHeight;
    public final double medianCharDensity;
    public final int totalPages;
    public final int dominantHeadingCharPatternType;
    public final Map<Integer, Integer> lineHashFrequencies; // For C-Factor

    public DocumentStats(Style dominantTextStyle, double medianLineHeight, double medianCharDensity, int totalPages, int dominantHeadingCharPatternType, Map<Integer, Integer> lineHashFrequencies) {
        this.dominantTextStyle = dominantTextStyle;
        this.medianLineHeight = medianLineHeight;
        this.medianCharDensity = medianCharDensity;
        this.totalPages = totalPages;
        this.dominantHeadingCharPatternType = dominantHeadingCharPatternType;
        this.lineHashFrequencies = lineHashFrequencies;
    }
}
