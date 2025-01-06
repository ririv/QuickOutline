package com.ririv.quickoutline.view.controls;

import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class SvgIconButton extends StackPane {

    private Button button;
    private SVGPath svgIcon;

    public SvgIconButton(String svgContent, double iconSize, String buttonText) {
        // 创建按钮
        button = new Button(buttonText);

        // 创建 SVG 图标
        svgIcon = new SVGPath();
        svgIcon.setContent(svgContent); // 设置 SVG 路径
        svgIcon.setFill(Color.BLACK);   // 默认填充颜色

        // 设置 SVG 图标大小
        svgIcon.setScaleX(iconSize);
        svgIcon.setScaleY(iconSize);

        // 将 SVG 图标和按钮组合
        getChildren().addAll(svgIcon, button);
        StackPane.setAlignment(svgIcon, javafx.geometry.Pos.CENTER_LEFT); // 图标在左边
        StackPane.setAlignment(button, javafx.geometry.Pos.CENTER);
    }

    // 修改 SVG 图标路径
    public void setSvgContent(String svgContent) {
        svgIcon.setContent(svgContent);
    }

    // 修改 SVG 图标颜色
    public void setSvgColor(Color color) {
        svgIcon.setFill(color);
    }

    // 获取按钮
    public Button getButton() {
        return button;
    }
}
