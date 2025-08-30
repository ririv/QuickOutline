package com.ririv.quickoutline.view.controls.slider;

import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class StyledSliderSkin extends SkinBase<StyledSlider> {

    private final StackPane track;
    private final Region coloredTrack;
    private final StackPane thumb;

    public StyledSliderSkin(StyledSlider slider) {
        super(slider);

        // Create the components
        track = new StackPane();
        track.getStyleClass().add("track");

        coloredTrack = new Region();
        coloredTrack.getStyleClass().add("colored-track");

        thumb = new StackPane();
        thumb.getStyleClass().add("thumb");

        // Add components to the skin
        getChildren().addAll(track, coloredTrack, thumb);

        // Add listeners to handle value changes and mouse events
        slider.valueProperty().addListener((obs, oldVal, newVal) -> getSkinnable().requestLayout());
        slider.setOnMousePressed(event -> {
            updateValue(event.getX());
        });
        slider.setOnMouseDragged(event -> {
            updateValue(event.getX());
        });
    }

    private void updateValue(double mouseX) {
        double newValue = getSkinnable().getMin() + (mouseX / getSkinnable().getWidth()) * (getSkinnable().getMax() - getSkinnable().getMin());
        if (newValue < getSkinnable().getMin()) {
            newValue = getSkinnable().getMin();
        } else if (newValue > getSkinnable().getMax()) {
            newValue = getSkinnable().getMax();
        }
        getSkinnable().setValue(newValue);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        double trackHeight = 4;
        double thumbSize = 14;

        // Layout the track
        track.resizeRelocate(x, y + (h - trackHeight) / 2, w, trackHeight);

        // Layout the thumb
        double thumbPos = (getSkinnable().getValue() - getSkinnable().getMin()) / (getSkinnable().getMax() - getSkinnable().getMin());
        double thumbX = x + thumbPos * w - thumbSize / 2;
        thumb.resizeRelocate(thumbX, y + (h - thumbSize) / 2, thumbSize, thumbSize);

        // Layout the colored track
        coloredTrack.resizeRelocate(x, y + (h - trackHeight) / 2, thumbX + thumbSize / 2, trackHeight);
    }
}
