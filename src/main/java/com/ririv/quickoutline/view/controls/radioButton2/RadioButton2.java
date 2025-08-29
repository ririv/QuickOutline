package com.ririv.quickoutline.view.controls.radioButton2;

import javafx.scene.control.ToggleButton;

import java.util.Objects;

public class RadioButton2 extends ToggleButton {

    public RadioButton2() {
        super();
        initialize();
    }

    public RadioButton2(String text) {
        super(text);
        initialize();
    }

    private void initialize() {
        getStyleClass().add("radio-button-style");
        String css = Objects.requireNonNull(getClass().getResource("RadioButton2.css")).toExternalForm();
        if (!getStylesheets().contains(css)) {
            getStylesheets().add(css);
        }
    }
}
