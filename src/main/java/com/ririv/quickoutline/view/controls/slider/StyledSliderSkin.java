package com.ririv.quickoutline.view.controls.slider;

import javafx.geometry.Bounds;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class StyledSliderSkin extends SkinBase<StyledSlider> {

    private final StackPane track;
    private final Region coloredTrack;
    private final StackPane thumb;
    private boolean isDragging = false;
    
    // 现代化风格的尺寸
    private static final double TRACK_HEIGHT = 4.0;
    private static final double THUMB_SIZE = 14.0;

    public StyledSliderSkin(StyledSlider slider) {
        super(slider);

        // Create the components
        track = new StackPane();
        track.getStyleClass().add("track");
        track.setMouseTransparent(false);

        coloredTrack = new Region();
        coloredTrack.getStyleClass().add("colored-track");
        coloredTrack.setMouseTransparent(true);

        thumb = new StackPane();
        thumb.getStyleClass().add("thumb");
        thumb.setMouseTransparent(false);

        // Add components to the skin
        getChildren().addAll(track, coloredTrack, thumb);

        // 添加值变化监听器
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            getSkinnable().requestLayout();
        });

        // 设置鼠标事件处理
        setupMouseHandlers();
    }

    private void setupMouseHandlers() {
        // Track 点击事件
        track.setOnMousePressed(this::handleTrackPressed);
        
        // Thumb 拖拽事件
        thumb.setOnMousePressed(this::handleThumbPressed);
        thumb.setOnMouseDragged(this::handleThumbDragged);
        thumb.setOnMouseReleased(this::handleMouseReleased);
        
        // 整个组件的鼠标事件
        getSkinnable().setOnMouseDragged(this::handleSliderDragged);
        getSkinnable().setOnMouseReleased(this::handleMouseReleased);
        
        // Hover 效果
        thumb.setOnMouseEntered(e -> thumb.getStyleClass().add("hover"));
        thumb.setOnMouseExited(e -> {
            if (!isDragging) {
                thumb.getStyleClass().remove("hover");
            }
        });
    }

    private void handleTrackPressed(MouseEvent event) {
        if (!getSkinnable().isDisabled()) {
            updateValueFromMouseEvent(event.getX());
            event.consume();
        }
    }

    private void handleThumbPressed(MouseEvent event) {
        if (!getSkinnable().isDisabled()) {
            isDragging = true;
            thumb.getStyleClass().add("pressed");
            event.consume();
        }
    }

    private void handleThumbDragged(MouseEvent event) {
        if (isDragging && !getSkinnable().isDisabled()) {
            // 将鼠标坐标转换为相对于整个滑块的坐标
            Bounds thumbBounds = thumb.getBoundsInParent();
            double mouseXInSlider = thumbBounds.getMinX() + event.getX();
            updateValueFromMouseEvent(mouseXInSlider);
            event.consume();
        }
    }

    private void handleSliderDragged(MouseEvent event) {
        if (isDragging && !getSkinnable().isDisabled()) {
            updateValueFromMouseEvent(event.getX());
            event.consume();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (isDragging) {
            isDragging = false;
            thumb.getStyleClass().remove("pressed");
            if (!thumb.isHover()) {
                thumb.getStyleClass().remove("hover");
            }
        }
    }

    private void updateValueFromMouseEvent(double mouseX) {
        StyledSlider slider = getSkinnable();
        double trackWidth = track.getWidth();
        
        if (trackWidth <= 0) {
            return;
        }
        
        // 确保鼠标位置在有效范围内
        double clampedMouseX = Math.max(0, Math.min(mouseX, trackWidth));
        
        // 计算新值（连续的，默认不使用步长约束）
        double percentage = clampedMouseX / trackWidth;
        double range = slider.getMax() - slider.getMin();
        double newValue = slider.getMin() + (percentage * range);
        
        // 只有在用户明确设置了 snapToTicks 为 true 时才应用步长约束
        // 默认情况下 snapToTicks 为 false，实现连续滑动
        if (slider.isSnapToTicks() && slider.getMajorTickUnit() > 0) {
            newValue = Math.round(newValue / slider.getMajorTickUnit()) * slider.getMajorTickUnit();
        }
        
        // 确保值在有效范围内
        newValue = Math.max(slider.getMin(), Math.min(slider.getMax(), newValue));
        
        slider.setValue(newValue);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        // 使用固定的滑块大小，不再根据状态变化
        double currentThumbSize = THUMB_SIZE;

        // 布局轨道
        double trackY = y + (h - TRACK_HEIGHT) / 2;
        track.resizeRelocate(x, trackY, w, TRACK_HEIGHT);

        // 计算滑块位置
        StyledSlider slider = getSkinnable();
        double range = slider.getMax() - slider.getMin();
        double thumbPos = range > 0 ? (slider.getValue() - slider.getMin()) / range : 0;
        
        // 确保滑块不会超出边界
        double thumbX = x + thumbPos * w - currentThumbSize / 2;
        thumbX = Math.max(x - currentThumbSize / 2, Math.min(x + w - currentThumbSize / 2, thumbX));
        
        double thumbY = y + (h - currentThumbSize) / 2;
        thumb.resizeRelocate(thumbX, thumbY, currentThumbSize, currentThumbSize);

        // 布局着色轨道（从开始到滑块位置）
        double coloredTrackWidth = Math.max(0, thumbX + currentThumbSize / 2 - x);
        coloredTrack.resizeRelocate(x, trackY, coloredTrackWidth, TRACK_HEIGHT);
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + THUMB_SIZE + rightInset;
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + Math.max(TRACK_HEIGHT, THUMB_SIZE) + bottomInset;
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + 200 + rightInset; // 默认宽度
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + Math.max(TRACK_HEIGHT, THUMB_SIZE) + bottomInset;
    }
}
