package com.ririv.quickoutline.api.model;

public class TocLinkDto {
    public int tocPageIndex; // TOC 自身的页码，从 0 开始
    public float x;
    public float y;
    public float width;
    public float height;
    public String targetPage; // 目标页码字符串，可能需要解析
}
