package com.ririv.quickoutline.view;

import com.ririv.quickoutline.pdfProcess.PdfPreview;
import com.ririv.quickoutline.view.controls.PopupCard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThumbnailViewController extends VBox {

    private static final double BASE_WIDTH = 150;
    private static final double BASE_HEIGHT = 225;
    private static final double POPUP_WIDTH = 600; // Width of the preview image

    @FXML
    private ImageView thumbnailImageView;
    @FXML
    private Label pageLabel;

    private Image originalImage; // This is the low-res image for the thumbnail
    private ImageView popupImageView; // The content for the popup
    private int pageIndex; // Store the page index to re-render high-res image
    private PdfPreview pdfPreviewInstance; // Store the PdfPreview instance
    private String[] pageLabels; // Store the page labels array
    private ExecutorService previewRenderExecutor = Executors.newSingleThreadExecutor(); // Executor for high-res rendering

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
        imagePopupCard.setTriggers(PopupCard.TriggerType.DELAYED_ON_HOVER, PopupCard.TriggerType.CTRL_ON_ENTER, PopupCard.TriggerType.CTRL_WHILE_HOVER);
        imagePopupCard.setHideDelay(Duration.millis(1));

        // 4. Attach its hover logic to this thumbnail component
        imagePopupCard.attachTo(this);
    }

    public void setScale(double scale) {
        thumbnailImageView.setFitWidth(BASE_WIDTH * scale);
        thumbnailImageView.setFitHeight(BASE_HEIGHT * scale);
    }

    public void setThumbnailImage(Image image, int pageIndex, PdfPreview pdfPreview, String[] pageLabels) {
        this.originalImage = image;
        this.pageIndex = pageIndex;
        this.pdfPreviewInstance = pdfPreview;
        this.pageLabels = pageLabels;
        this.thumbnailImageView.setImage(image);

        // Asynchronously render high-res image for popup
        if (popupImageView != null && pdfPreviewInstance != null) {
            previewRenderExecutor.submit(() -> {
                try {
                    pdfPreviewInstance.renderPreviewImage(pageIndex, highResImage -> {
                        Platform.runLater(() -> popupImageView.setImage(highResImage));
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // Update page label with PageLabelService
        updatePageLabel();
    }

    private void updatePageLabel() {
        if (pageLabels != null && pageIndex < pageLabels.length) {
            String labelText = pageLabels[pageIndex];
            if (labelText != null && !labelText.isEmpty()) {
                pageLabel.setText(labelText);
            } else {
                pageLabel.setText("第 " + (pageIndex + 1) + " 页");
            }
        } else {
            pageLabel.setText("第 " + (pageIndex + 1) + " 页");
        }
    }

    public void setPageLabel(String label) {
        // This method is now deprecated, updatePageLabel() should be used instead.
        // Keeping it for compatibility if other parts of the code still call it.
        pageLabel.setText(label);
    }
}