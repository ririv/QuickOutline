package com.ririv.quickoutline.api.state;

import com.ririv.quickoutline.model.Bookmark;

/**
 * Holds the current state of the Bookmark editing session for the API layer.
 * Acts as the Single Source of Truth for the frontend.
 */
public class ApiBookmarkState {
    private Bookmark rootBookmark;
    private int offset = 0;

    public Bookmark getRootBookmark() {
        return rootBookmark;
    }

    public void setRootBookmark(Bookmark rootBookmark) {
        this.rootBookmark = rootBookmark;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public void clear() {
        this.rootBookmark = null;
        this.offset = 0;
    }
    
    public boolean hasRootBookmark() {
        return this.rootBookmark != null;
    }
}
