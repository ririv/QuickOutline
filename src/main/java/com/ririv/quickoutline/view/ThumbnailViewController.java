package com.ririv.quickoutline.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

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

    public void setThumbnailImage(Image image) {
        thumbnailImageView.setImage(image);
    }

    public void setPageLabel(String label) {
        pageLabel.setText(label);
    }
}
