package com.ririv.quickoutline.view.controls;

import com.ririv.quickoutline.view.utils.BatikToJavaFXConverter;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A base class for creating icons from SVG files using -fx-shape.
 */
public class SvgIcon extends Region {

    /**
     * Sets the SVG path for this icon.
     * @param svgPath The resource path to the SVG file.
     */
    protected void setSvgPath(String svgPath) {
        String combinedPath = BatikToJavaFXConverter.getCombinedPath(svgPath);
        if (!combinedPath.isEmpty()) {
            this.setStyle("-fx-shape: \"" + combinedPath + "\";");
        }
        // Add a default style class for all icons
        this.getStyleClass().add("icon");

        setBackground(Background.fill(Color.GRAY));
    }
}
