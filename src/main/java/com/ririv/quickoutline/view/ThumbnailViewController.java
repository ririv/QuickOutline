package com.ririv.quickoutline.view;

import com.ririv.quickoutline.view.controls.PopupCard;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class ThumbnailViewController extends VBox {

    private static final double BASE_WIDTH = 150;
    private static final double BASE_HEIGHT = 225;
    private static final double POPUP_WIDTH = 600; // Width of the preview image

    @FXML
    private ImageView thumbnailImageView;
    @FXML
    private Label pageLabel;

    private Image originalImage;
    private ImageView popupImageView; // The content for the popup

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

        // Prepare the popup card and its content
        setupPopupCard();
    }

    private void setupPopupCard() {
        // 1. Create the content for the popup
        popupImageView = new ImageView();
        popupImageView.setPreserveRatio(true);
        popupImageView.setFitWidth(POPUP_WIDTH);

        // 2. The PopupCard requires a Parent node, so we wrap the ImageView in a StackPane.
        StackPane popupContentWrapper = new StackPane(popupImageView);

        // 3. Create and configure the PopupCard
        PopupCard imagePopupCard = new PopupCard(popupContentWrapper);
        imagePopupCard.setPosition(PopupCard.PopupPosition.RIGHT_OF);
        imagePopupCard.setTriggers(PopupCard.TriggerType.DELAYED_ON_HOVER, PopupCard.TriggerType.CTRL_ON_HOVER);
        imagePopupCard.setHideDelay(Duration.millis(1));

        // 4. Attach its hover logic to this thumbnail component
        imagePopupCard.attachTo(this);
    }

    public void setScale(double scale) {
        thumbnailImageView.setFitWidth(BASE_WIDTH * scale);
        thumbnailImageView.setFitHeight(BASE_HEIGHT * scale);
    }

    public void setThumbnailImage(Image image) {
        this.originalImage = image;
        this.thumbnailImageView.setImage(image);

        // Also set the image for the popup view so it's ready when shown
        if (popupImageView != null) {
            popupImageView.setImage(originalImage);
        }
    }

    public void setPageLabel(String label) {
        pageLabel.setText(label);
    }
}
