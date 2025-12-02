package com.ririv.quickoutline.api.state;


/// TODO
public class CurrentFileState {
    private String filePath;
    private int pageCount = -1;

    public void open(String path) {
        this.filePath = path;
        this.pageCount = -1; // Reset cache
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isOpen() {
        return filePath != null && !filePath.trim().isEmpty();
    }

    public void clear() {
        this.filePath = null;
        this.pageCount = -1;
    }

    public void setPageCount(int count) {
        this.pageCount = count;
    }

    public int getPageCount() {
        return pageCount;
    }
}