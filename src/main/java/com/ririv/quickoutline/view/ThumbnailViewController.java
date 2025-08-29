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

        // Listen for bounds changes to apply the clip.
        thumbnailImageView.boundsInLocalProperty().addListener((obs, oldBounds, newBounds) -> {
            // Only apply the clip when the ImageView has valid, non-zero bounds.
            if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
                // Set the clip on the ImageView if it hasn't been set already.
                if (thumbnailImageView.getClip() == null) {
                    thumbnailImageView.setClip(clip);
                }
                // Update the clip's size to match the new bounds.
                clip.setWidth(newBounds.getWidth());
                clip.setHeight(newBounds.getHeight());
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

