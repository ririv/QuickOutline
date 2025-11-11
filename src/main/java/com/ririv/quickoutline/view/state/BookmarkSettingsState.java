package com.ririv.quickoutline.view.state;

import com.ririv.quickoutline.model.Bookmark;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BookmarkSettingsState {
    private final IntegerProperty offset = new SimpleIntegerProperty(0);
    private final ObjectProperty<Bookmark> rootBookmark = new SimpleObjectProperty<>();

    public int getOffset() {
        return offset.get();
    }

    public IntegerProperty offsetProperty() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset.set(offset);
    }

    public Bookmark getRootBookmark() {
        return rootBookmark.get();
    }

    public ObjectProperty<Bookmark> rootBookmarkProperty() {
        return rootBookmark;
    }

    public void setRootBookmark(Bookmark rootBookmark) {
        this.rootBookmark.set(rootBookmark);
    }
}
