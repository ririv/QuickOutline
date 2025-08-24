package com.ririv.quickoutline.view.controls.select;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

public class StyledSelect<T> extends Control {

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObjectProperty<T> value = new SimpleObjectProperty<>();

    public StyledSelect() {
        getStyleClass().add("styled-select");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new StyledSelectSkin<>(this);
    }

    // Items
    public ObservableList<T> getItems() {
        return items;
    }

    public void setItems(ObservableList<T> items) {
        this.items.setAll(items);
    }

    // Value
    public T getValue() {
        return value.get();
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public void setValue(T value) {
        this.value.set(value);
    }
}
