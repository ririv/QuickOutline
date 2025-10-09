package com.ririv.quickoutline.pdfProcess;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Per-file rendering session with a single PDDocument and a single-thread daemon executor.
 * Consumers submit rendering tasks via renderThumbnailAsync/renderPreviewAsync and receive callbacks.
 */
public class PdfRenderSession implements AutoCloseable {

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final ExecutorService executor;

    private static final float THUMBNAIL_DPI = 72f;
    private static final float PREVIEW_DPI = 300f;

    public PdfRenderSession(File file) throws IOException {
        this.document = Loader.loadPDF(file);
        this.renderer = new PDFRenderer(document);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "page-render-session");
            t.setDaemon(true);
            return t;
        });
    }

    public int getPageCount() {
        return document.getNumberOfPages();
    }

    public void renderThumbnailAsync(int pageIndex, Consumer<BufferedImage> callback) {
        renderAsync(pageIndex, THUMBNAIL_DPI, callback);
    }

    public void renderPreviewAsync(int pageIndex, Consumer<BufferedImage> callback) {
        renderAsync(pageIndex, PREVIEW_DPI, callback);
    }

    public void renderAsync(int pageIndex, float dpi, Consumer<BufferedImage> callback) {
        if (pageIndex < 0 || pageIndex >= getPageCount()) return;
        if (executor.isShutdown()) return;
        executor.submit(() -> {
            try {
                BufferedImage img = renderer.renderImageWithDPI(pageIndex, dpi);
                if (callback != null) callback.accept(img);
            } catch (IOException e) {
                // Swallow to keep background thread healthy; callers can handle nulls if needed
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
