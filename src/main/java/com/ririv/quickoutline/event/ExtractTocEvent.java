package com.ririv.quickoutline.event;

public class ExtractTocEvent {
    public final Integer startPage;
    public final Integer endPage;

    public ExtractTocEvent(Integer startPage, Integer endPage) {
        this.startPage = startPage;
        this.endPage = endPage;
    }
}
