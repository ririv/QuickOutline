package com.ririv.quickoutline.view.controls.slider;

import javafx.scene.control.Slider;
import javafx.scene.control.Skin;

import java.util.Objects;

/**
 * 现代风格的自定义滑块组件
 * 支持平滑动画、悬停效果和精确的值控制
 */
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
        
        // 加载CSS样式文件
        String cssPath = "/com/ririv/quickoutline/view/controls/slider/StyledSlider.css";
        String cssUrl = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
        getStylesheets().add(cssUrl);
        
        // 设置默认属性（连续滑动，不吸附到刻度）
        setBlockIncrement(1.0);
        setMajorTickUnit(1.0);  // 设置为1，但不启用吸附
        setSnapToTicks(false);  // 不吸附到刻度，实现连续滑动
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new StyledSliderSkin(this);
    }
    
    /**
     * 设置滑块的颜色主题
     * @param theme 主题名称："default", "success", "warning", "error"
     */
    public void setTheme(String theme) {
        getStyleClass().removeAll("success", "warning", "error");
        if (!"default".equals(theme)) {
            getStyleClass().add(theme);
        }
    }
}