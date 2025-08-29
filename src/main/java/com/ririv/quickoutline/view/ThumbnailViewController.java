package com.ririv.quickoutline.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class ThumbnailViewController extends VBox {

    private static final double BASE_WIDTH = 150;
    private static final double BASE_HEIGHT = 225;

    @FXML
    private ImageView thumbnailImageView; // This will display the final snapshot

    @FXML
    private Label pageLabel;

    private Image originalImage; // Store the original, square-cornered image

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
        // Set base size. The displayed image view itself has no clip.
        thumbnailImageView.setFitWidth(BASE_WIDTH);
        thumbnailImageView.setFitHeight(BASE_HEIGHT);

        // Listener to dynamically update the VBox container's height based on the final image's bounds.
        thumbnailImageView.boundsInLocalProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
                double labelHeight = 20; // Estimate label height
                double totalHeight = newBounds.getHeight() + getSpacing() + getPadding().getTop() + getPadding().getBottom() + labelHeight;
                setPrefHeight(totalHeight);
            }
        });
    }

    public void setScale(double scale) {
        thumbnailImageView.setFitWidth(BASE_WIDTH * scale);
        thumbnailImageView.setFitHeight(BASE_HEIGHT * scale);
        // When scale changes, we need to re-generate the snapshot with the new dimensions.
        updateSnapshot();
    }

    public void setThumbnailImage(Image image) {
        this.originalImage = image;
        updateSnapshot();
    }

    private void updateSnapshot() {
        if (originalImage == null) {
            thumbnailImageView.setImage(null);
            return;
        }

        // 1. Create a temporary ImageView in memory to apply effects to.
        ImageView sourceView = new ImageView(originalImage);
        sourceView.setPreserveRatio(true);
        sourceView.setFitWidth(thumbnailImageView.getFitWidth());
        sourceView.setFitHeight(thumbnailImageView.getFitHeight());

        // 2. Take a dummy snapshot to force a layout pass and compute the bounds.
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        sourceView.snapshot(params, null);

        // 3. Create the clip and size it to the now-correct layout bounds.
        Rectangle clip = new Rectangle();
                clip.setArcWidth(15);
        clip.setArcHeight(15);
        clip.setWidth(sourceView.getLayoutBounds().getWidth());
        clip.setHeight(sourceView.getLayoutBounds().getHeight());
        sourceView.setClip(clip);

        // 4. Take the real snapshot with the correctly sized clip.
        WritableImage snapshot = sourceView.snapshot(params, null);

        // 5. Set the final, pre-rendered, rounded-corner image to the visible ImageView.
        thumbnailImageView.setImage(snapshot);
    }

    public void setPageLabel(String label) {
        pageLabel.setText(label);
    }
}
