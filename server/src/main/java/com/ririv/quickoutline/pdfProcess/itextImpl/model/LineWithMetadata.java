package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import com.itextpdf.kernel.geom.Rectangle;

import java.util.List;

// 视觉上的“行”
public class LineWithMetadata {
    private final String textContent;
    private final float x;
    private final float y;
    private final float width;
    private final Style style;
    private final int pageNum;
    private final Rectangle pageSize;
    private final double skew;
    private final List<TextChunk> chunks;

    public LineWithMetadata(String textContent, float x, float y, float width, Style style, int pageNum, Rectangle pageSize, double skew, List<TextChunk> chunks) {
        this.textContent = textContent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.style = style;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.skew = skew;
        this.chunks = chunks;
    }

    // Getters
    public String getTextContent() { return textContent; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public Style getStyle() { return style; }
    public int getPageNum() { return pageNum; }
    public Rectangle getPageSize() { return pageSize; }
    public double getSkew() { return skew; }
    public List<TextChunk> getChunks() { return chunks; }
}