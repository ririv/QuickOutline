package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import com.itextpdf.kernel.geom.Rectangle;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.Style;

public class LineWithMetadata {
    private final String textContent;
    private final float x;
    private final float y;
    private final float width;
    private final Style style;
    private final int pageNum;
    private final Rectangle pageSize;
    private final double skew;

    public LineWithMetadata(String textContent, float x, float y, float width, Style style, int pageNum, Rectangle pageSize, double skew) {
        this.textContent = textContent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.style = style;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.skew = skew;
    }

    public String getTextContent() {
        return textContent;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public Style getStyle() {
        return style;
    }

    public int getPageNum() {
        return pageNum;
    }

    public Rectangle getPageSize() {
        return pageSize;
    }

    public double getSkew() {
        return skew;
    }
}
