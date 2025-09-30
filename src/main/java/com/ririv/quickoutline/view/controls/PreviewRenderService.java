package com.ririv.quickoutline.view.controls;

import com.ririv.quickoutline.pdfProcess.PageImageRender;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class PreviewRenderService {

    private final PageImageRender pageImageRender;
    private final ExecutorService executor;

    public PreviewRenderService(PageImageRender pageImageRender, ExecutorService executor) {
        this.pageImageRender = pageImageRender;
        this.executor = executor;
    }

    public void renderPreview(int pageIndex, Consumer<Image> onComplete) {
        if (pageImageRender == null || executor == null || executor.isShutdown()) {
            return;
        }
        executor.submit(() -> {
            try {
                pageImageRender.renderPreviewImage(pageIndex, bufferedImage -> {
                    Image highResImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    Platform.runLater(() -> onComplete.accept(highResImage));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
