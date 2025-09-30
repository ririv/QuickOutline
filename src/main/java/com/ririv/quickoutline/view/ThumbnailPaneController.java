package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.pdfProcess.PageImageRender;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.view.state.CurrentFileState;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.TilePane;

import java.io.File;
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
    private PageImageRender currentPreview;
    private String[] currentPageLabels; // Store the page labels array
    private ExecutorService fileLoadExecutor = Executors.newSingleThreadExecutor();
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

        currentFileState.srcFileProperty().addListener((obs, oldPath, newPath) -> {
            if (newPath != null) {
                loadPdf(newPath.toFile());
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

    private void loadPdf(File pdfFile) {
        reset();

        Task<PageImageRender> loadFileTask = new Task<>() {
            @Override
            protected PageImageRender call() throws Exception {
                return new PageImageRender(pdfFile);
            }
        };

        loadFileTask.setOnSucceeded(event -> {
            currentPreview = loadFileTask.getValue();
            // 获取页码标签
            try {
                currentPageLabels = pdfPageLabelService.getPageLabels(currentFileState.getSrcFile().toString());
            } catch (IOException e) {
                e.printStackTrace();
                currentPageLabels = null; // Fallback to default numbering
            }
            thumbnailRenderExecutor = Executors.newSingleThreadExecutor();
            currentPageIndex = 0;
            loadMoreThumbnails(); // Load the first batch
        });

        loadFileTask.setOnFailed(event -> {
            loadFileTask.getException().printStackTrace();
            reset();
        });

        fileLoadExecutor.submit(loadFileTask);
    }

    public void setCurrentPageLabels(String[] pageLabels) {
        this.currentPageLabels = pageLabels;
    }

    private void loadMoreThumbnails() {
        if (currentPreview == null || isLoading || currentPageIndex >= currentPreview.getPageCount()) {
            return;
        }
        isLoading = true;

        int limit = Math.min(currentPageIndex + BATCH_SIZE, currentPreview.getPageCount());

        for (int i = currentPageIndex; i < limit; i++) {
            final int pageIndex = i;
            ThumbnailView thumbnailView = new ThumbnailView();
            thumbnailView.setScale(currentScale); // Apply current scale to new thumbnails
            thumbnailTilePane.getChildren().add(thumbnailView);

            if (thumbnailRenderExecutor == null || thumbnailRenderExecutor.isShutdown()) return;
            
            thumbnailRenderExecutor.submit(() -> {
                try {
                    currentPreview.renderThumbnail(pageIndex, bufferedImage -> {
                        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                        Platform.runLater(() -> thumbnailView.setThumbnailImage(image, pageIndex, currentPreview, currentPageLabels));
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        if (currentPreview != null) {
            try {
                currentPreview.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            currentPreview = null;
        }
        currentPageIndex = 0;
        isLoading = false;
        // Reset slider and scale on file change
        zoomSlider.setValue(1.0);
        currentScale = 1.0;
    }
}