package com.ririv.quickoutline.view;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;

public class GraphButtonWrapper extends StackPane {

    // 定义 padding 属性
    @FXML
    private final ObjectProperty<Double> wrapperPadding = new SimpleObjectProperty<>(this, null,5.0);


    public GraphButtonWrapper() {
        this.setPadding(new Insets(wrapperPadding.get()));
        this.wrapperPadding.addListener((observable, oldValue, newValue) -> {
            this.setPadding(new Insets(wrapperPadding.get()));
        });
    }


    public Double getWrapperPadding() {
        return wrapperPadding.get();
    }

    public void setWrapperPadding(Double padding) {
        this.wrapperPadding.set(padding);
    }

    public ObjectProperty<Double> wrapperPaddingProperty() {
        return wrapperPadding;
    }
}
