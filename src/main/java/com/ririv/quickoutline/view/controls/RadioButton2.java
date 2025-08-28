package com.ririv.quickoutline.view.controls;

import javafx.scene.control.ToggleButton;

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
        String css = getClass().getResource("RadioButton.css").toExternalForm();
        if (!getStylesheets().contains(css)) {
            getStylesheets().add(css);
        }
    }
}
