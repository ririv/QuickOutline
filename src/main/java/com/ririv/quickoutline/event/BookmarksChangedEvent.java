package com.ririv.quickoutline.event;

import com.ririv.quickoutline.model.Bookmark;

public class BookmarksChangedEvent {

    private final Bookmark rootBookmark;

    public BookmarksChangedEvent(Bookmark rootBookmark) {
        this.rootBookmark = rootBookmark;
    }

    public Bookmark getRootBookmark() {
        return rootBookmark;
    }
}
