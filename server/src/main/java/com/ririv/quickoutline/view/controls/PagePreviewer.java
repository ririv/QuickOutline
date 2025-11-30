package com.ririv.quickoutline.view.controls;

import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.function.Supplier;

public class PagePreviewer {

    private static final double POPUP_WIDTH = 600;
    private final ImageView popupImageView;
    private final PopupCard imagePopupCard;
    private PdfRenderSession renderSession;

    public PagePreviewer() {
        this.popupImageView = new ImageView();
        this.popupImageView.setPreserveRatio(true);
        this.popupImageView.setFitWidth(POPUP_WIDTH);

        StackPane popupContentWrapper = new StackPane(popupImageView);

        this.imagePopupCard = new PopupCard(popupContentWrapper);
        this.imagePopupCard.setPosition(PopupCard.PopupPosition.RIGHT_OF);
        this.imagePopupCard.setTriggers(PopupCard.TriggerType.INSTANT_ON_HOVER);
        this.imagePopupCard.setHideDelay(Duration.millis(1));
    }

    public void setRenderSession(PdfRenderSession session) {
        this.renderSession = session;
    }

    public void attach(Node node, Supplier<Integer> pageIndexSupplier) {
        imagePopupCard.attachTo(node);
        node.setOnMouseEntered(event -> {
            if (renderSession != null) {
                Integer pageIndex = pageIndexSupplier.get();
                if (pageIndex != null && pageIndex >= 0 && pageIndex < renderSession.getPageCount()) {
                    renderSession.renderPreviewAsync(pageIndex, bufferedImage -> {
                        Image highResImage = SwingFXUtils.toFXImage(bufferedImage, null);
                        Platform.runLater(() -> popupImageView.setImage(highResImage));
                    });
                } else {
                    imagePopupCard.hide();
                    Platform.runLater(() -> popupImageView.setImage(null));
                }
            } else {
                imagePopupCard.hide();
            }
        });
    }
}
