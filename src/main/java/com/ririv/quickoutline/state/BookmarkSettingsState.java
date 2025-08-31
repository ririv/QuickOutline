package com.ririv.quickoutline.state;

import com.ririv.quickoutline.model.Bookmark;

public class BookmarkSettingsState {
    private Integer offset;
    private Bookmark rootBookmark;

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Bookmark getRootBookmark() {
        return rootBookmark;
    }

    public void setRootBookmark(Bookmark rootBookmark) {
        this.rootBookmark = rootBookmark;
    }
}

