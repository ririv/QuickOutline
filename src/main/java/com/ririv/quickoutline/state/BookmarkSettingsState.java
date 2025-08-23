package com.ririv.quickoutline.state;

import com.ririv.quickoutline.model.Bookmark;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BookmarkSettingsState {
    private final ObjectProperty<Integer> offset = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Bookmark> rootBookmark = new SimpleObjectProperty<>();

    public Integer getOffset() {
        return offset.get();
    }

    public ObjectProperty<Integer> offsetProperty() {
        return offset;
    }

    public void setOffset(Integer offset) {
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
