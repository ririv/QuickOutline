package com.ririv.quickoutline.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class ThumbnailViewController extends VBox {

    @FXML
    private ImageView thumbnailImageView;

    @FXML
    private Label pageLabel;

    public ThumbnailViewController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ThumbnailView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(15);
        clip.setArcHeight(15);

        // Listen for bounds changes to apply the clip and update container height.
        thumbnailImageView.boundsInLocalProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
                // 1. Apply the clip to the ImageView.
                if (thumbnailImageView.getClip() == null) {
                    thumbnailImageView.setClip(clip);
                }
                clip.setWidth(newBounds.getWidth());
                clip.setHeight(newBounds.getHeight());

                // 2. Dynamically set the preferred height of this VBox container.
                // This ensures the parent TilePane allocates the correct space, preventing clipping.
                double labelHeight = 20; // Estimate label height
                double totalHeight = newBounds.getHeight() + getSpacing() + getPadding().getTop() + getPadding().getBottom() + labelHeight;
                setPrefHeight(totalHeight);
            }
        });
    }

    public void setThumbnailImage(Image image) {
        // When the image is cleared, also remove the clip to prevent issues.
        if (image == null) {
            thumbnailImageView.setClip(null);
        }
        thumbnailImageView.setImage(image);
    }

    public void setPageLabel(String label) {
        pageLabel.setText(label);
    }
}

