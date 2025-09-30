package com.ririv.quickoutline.view.controls;

import com.ririv.quickoutline.pdfProcess.PageImageRender;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class PagePreviewer {

    private static final double POPUP_WIDTH = 600;
    private final ImageView popupImageView;
    private final PopupCard imagePopupCard;
    private ExecutorService previewRenderExecutor;
    private PageImageRender pageImageRender;

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

    public void setPageImageRender(PageImageRender pageImageRender) {
        if (this.previewRenderExecutor != null) {
            this.previewRenderExecutor.shutdownNow();
        }
        this.pageImageRender = pageImageRender;
        if (this.pageImageRender != null) {
            this.previewRenderExecutor = Executors.newSingleThreadExecutor();
        } else {
            this.previewRenderExecutor = null;
        }
    }

    public void attach(Node node, Supplier<Integer> pageIndexSupplier) {
        imagePopupCard.attachTo(node);
        node.setOnMouseEntered(event -> {
            if (pageImageRender != null && previewRenderExecutor != null && !previewRenderExecutor.isShutdown()) {
                Integer pageIndex = pageIndexSupplier.get();
                if (pageIndex != null && pageIndex >= 0 && pageIndex < pageImageRender.getPageCount()) {
                    previewRenderExecutor.submit(() -> {
                        try {
                            pageImageRender.renderPreviewImage(pageIndex, bufferedImage -> {
                                Image highResImage = SwingFXUtils.toFXImage(bufferedImage, null);
                                Platform.runLater(() -> popupImageView.setImage(highResImage));
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
    
    // It's good practice to provide a way to shut down the executor when the previewer is no longer needed.
    public void shutdown() {
        if (previewRenderExecutor != null) {
            previewRenderExecutor.shutdownNow();
        }
    }
}
