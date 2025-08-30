package com.ririv.quickoutline.view.controls.slider;

import javafx.scene.control.Skin;
import javafx.scene.control.Slider;

import java.util.Objects;

public class StyledSlider extends Slider {

    public StyledSlider() {
        super();
        init();
    }

    public StyledSlider(double min, double max, double value) {
        super(min, max, value);
        init();
    }

    private void init() {
        getStyleClass().add("styled-slider");
        getStylesheets().add(Objects.requireNonNull(getClass().getResource("StyledSlider.css")).toExternalForm());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new StyledSliderSkin(this);
    }
}