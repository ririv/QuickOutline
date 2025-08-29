package com.ririv.quickoutline.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ThumbnailViewController extends VBox {

    private static final double BASE_WIDTH = 150;
    private static final double BASE_HEIGHT = 225;

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
        // Set base size
        thumbnailImageView.setFitWidth(BASE_WIDTH);
        thumbnailImageView.setFitHeight(BASE_HEIGHT);

        // Listener to dynamically update the VBox container's height.
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
    }

    public void setThumbnailImage(Image image) {
        thumbnailImageView.setImage(image);
    }

    public void setPageLabel(String label) {
        pageLabel.setText(label);
    }
}