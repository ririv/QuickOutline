package com.ririv.quickoutline.view;

import jakarta.inject.Inject;
import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.view.state.CurrentFileState;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.Subscribe;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.PageLabelsChangedEvent;

public class ThumbnailPaneController {

    private static final int BATCH_SIZE = 20; // Number of thumbnails to load at a time

    @FXML
    private Slider zoomSlider;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TilePane thumbnailTilePane;

    private final CurrentFileState currentFileState;
    private final PdfPageLabelService pdfPageLabelService; // Inject PdfPageLabelService
    private final AppEventBus appEventBus;
    private PdfRenderSession currentSession;
    private String[] currentPageLabels; // Store the page labels array
    private ExecutorService thumbnailRenderExecutor;
    private int currentPageIndex = 0;
    private boolean isLoading = false;
    private double currentScale = 1.0;

    @Inject
    public ThumbnailPaneController(CurrentFileState currentFileState, PdfPageLabelService pdfPageLabelService, AppEventBus appEventBus) {
        this.currentFileState = currentFileState;
        this.pdfPageLabelService = pdfPageLabelService;
        this.appEventBus = appEventBus;
        this.appEventBus.register(this);
    }

    @FXML
    public void initialize() {
        thumbnailTilePane.prefWidthProperty().bind(scrollPane.widthProperty());

        currentFileState.pageRenderSessionProperty().addListener((obs, oldSession, newSession) -> {
            if (newSession != null) {
                startWithSession(newSession);
            } else {
                reset();
            }
        });

        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.98 && !isLoading) {
                loadMoreThumbnails();
            }
        });

        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentScale = newVal.doubleValue();
            for (Node node : thumbnailTilePane.getChildren()) {
                if (node instanceof ThumbnailView) {
                    ((ThumbnailView) node).setScale(currentScale);
                }
            }
        });
    }

    @Subscribe
    public void onPageLabelsChanged(PageLabelsChangedEvent event) {
        Platform.runLater(() -> {
            currentPageLabels = event.getPageLabels().toArray(new String[0]);
            for (Node node : thumbnailTilePane.getChildren()) {
                if (node instanceof ThumbnailView) {
                    ((ThumbnailView) node).updatePageLabel(currentPageLabels);
                }
            }
        });
    }

    private void startWithSession(PdfRenderSession session) {
        reset();
        this.currentSession = session;
        // Fetch page labels asynchronously to avoid blocking FX thread
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "page-label-fetch");
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            String[] labels = null;
            try {
                if (currentFileState.getSrcFile() != null) {
                    labels = pdfPageLabelService.getPageLabels(currentFileState.getSrcFile().toString());
                }
            } catch (IOException ignored) {
            }
            final String[] result = labels;
            Platform.runLater(() -> {
                currentPageLabels = result;
                // Update existing thumbnails' labels if any
                for (Node node : thumbnailTilePane.getChildren()) {
                    if (node instanceof ThumbnailView tv) {
                        tv.updatePageLabel(currentPageLabels);
                    }
                }
            });
        });
        thumbnailRenderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "thumbnail-render");
            t.setDaemon(true);
            return t;
        });
        currentPageIndex = 0;
        loadMoreThumbnails(); // Load the first batch
    }

    public void setCurrentPageLabels(String[] pageLabels) {
        this.currentPageLabels = pageLabels;
    }

    private void loadMoreThumbnails() {
        final PdfRenderSession session = this.currentSession; // snapshot to avoid races
        if (session == null || isLoading || currentPageIndex >= session.getPageCount()) {
            return;
        }
        isLoading = true;

        int limit = Math.min(currentPageIndex + BATCH_SIZE, session.getPageCount());

        for (int i = currentPageIndex; i < limit; i++) {
            final int pageIndex = i;
            ThumbnailView thumbnailView = new ThumbnailView();
            thumbnailView.setScale(currentScale); // Apply current scale to new thumbnails
            thumbnailTilePane.getChildren().add(thumbnailView);

            if (thumbnailRenderExecutor == null || thumbnailRenderExecutor.isShutdown()) return;
            
            thumbnailRenderExecutor.submit(() -> {
                session.renderThumbnailAsync(pageIndex, bufferedImage -> {
                    Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                    Platform.runLater(() -> thumbnailView.setThumbnailImage(image, pageIndex, session, currentPageLabels));
                });
            });
        }
        currentPageIndex = limit;
        isLoading = false;
    }


    private void reset() {
        if (thumbnailRenderExecutor != null && !thumbnailRenderExecutor.isShutdown()) {
            thumbnailRenderExecutor.shutdownNow();
        }
        thumbnailTilePane.getChildren().clear();
        // session is owned by CurrentFileState; don't close here
        currentSession = null;
        currentPageIndex = 0;
        isLoading = false;
        // Reset slider and scale on file change
        zoomSlider.setValue(1.0);
        currentScale = 1.0;
    }
}