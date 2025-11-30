package com.ririv.quickoutline.service.atomic;

public class DocumentBlock {
    public String id;
    public String html;
    public String svgContent;
    public float heightPt;

    // 新增：真实的边距信息
    public float marginTop;
    public float marginBottom;

    public int pageNumber;
    public float topPt;

    public DocumentBlock(String html) {
        this.html = html;
    }
}