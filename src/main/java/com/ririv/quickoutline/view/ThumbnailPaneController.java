package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.pdfProcess.PdfPreview;
import com.ririv.quickoutline.state.CurrentFileState;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.TilePane;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThumbnailPaneController {

    private static final int BATCH_SIZE = 20; // Number of thumbnails to load at a time

    @FXML
    private Slider zoomSlider;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TilePane thumbnailTilePane;

    private final CurrentFileState currentFileState;
    private PdfPreview currentPreview;
    private ExecutorService fileLoadExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService thumbnailRenderExecutor;
    private int currentPageIndex = 0;
    private boolean isLoading = false;
    private double currentScale = 1.0;

    @Inject
    public ThumbnailPaneController(CurrentFileState currentFileState) {
        this.currentFileState = currentFileState;
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
                if (node instanceof ThumbnailViewController) {
                    ((ThumbnailViewController) node).setScale(currentScale);
                }
            }
        });
    }

    private void loadPdf(File pdfFile) {
        reset();

        Task<PdfPreview> loadFileTask = new Task<>() {
            @Override
            protected PdfPreview call() throws Exception {
                // This is the slow part, running in the background
                return new PdfPreview(pdfFile);
            }
        };

        loadFileTask.setOnSucceeded(event -> {
            // This runs on the FX thread after the task is successful
            currentPreview = loadFileTask.getValue();
            thumbnailRenderExecutor = Executors.newSingleThreadExecutor();
            currentPageIndex = 0;
            loadMoreThumbnails(); // Load the first batch
        });

        loadFileTask.setOnFailed(event -> {
            // Handle exceptions during PDF loading
            loadFileTask.getException().printStackTrace();
            reset();
        });

        fileLoadExecutor.submit(loadFileTask);
    }

    private void loadMoreThumbnails() {
        if (currentPreview == null || isLoading || currentPageIndex >= currentPreview.getPageCount()) {
            return;
        }
        isLoading = true;

        int limit = Math.min(currentPageIndex + BATCH_SIZE, currentPreview.getPageCount());

        for (int i = currentPageIndex; i < limit; i++) {
            final int pageIndex = i;
            ThumbnailViewController thumbnailView = new ThumbnailViewController();
            thumbnailView.setScale(currentScale); // Apply current scale to new thumbnails
            thumbnailView.setPageLabel("第 " + (pageIndex + 1) + " 页");
            thumbnailTilePane.getChildren().add(thumbnailView);

            if (thumbnailRenderExecutor == null || thumbnailRenderExecutor.isShutdown()) return;
            
            thumbnailRenderExecutor.submit(() -> {
                try {
                    if (Thread.currentThread().isInterrupted()) return;

                    currentPreview.renderThumbnail(pageIndex, image -> {
                        Platform.runLater(() -> thumbnailView.setThumbnailImage(image));
                    });

                } catch (Exception e) {
                    if (!(e instanceof InterruptedException || e.getCause() instanceof InterruptedException)) {
                        Platform.runLater(() -> {
                            thumbnailView.setPageLabel("页面 " + (pageIndex + 1) + " 加载失败");
                        });
                    }
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

