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
     * Sets the SVG from a classpath resource (preferred inside jpackage image).
     * @param resourcePath classpath resource path like "/drawable/open.svg"
     */
    protected void setSvgResource(String resourcePath) {
        String combinedPath = BatikToJavaFXConverter.getCombinedPathFromResource(resourcePath);
        if (!combinedPath.isEmpty()) {
            this.setStyle("-fx-shape: \"" + combinedPath + "\";");
        }
        this.getStyleClass().add("icon");
        setBackground(Background.fill(Color.GRAY));
    }
}
