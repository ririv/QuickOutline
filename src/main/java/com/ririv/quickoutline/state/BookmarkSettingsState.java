package com.ririv.quickoutline.state;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BookmarkSettingsState {
    private final ObjectProperty<Integer> offset = new SimpleObjectProperty<>(null);

    public Integer getOffset() {
        return offset.get();
    }

    public ObjectProperty<Integer> offsetProperty() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset.set(offset);
    }
}
