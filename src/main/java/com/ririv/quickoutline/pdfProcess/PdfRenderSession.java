package com.ririv.quickoutline.pdfProcess;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PdfRenderSession implements AutoCloseable {

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final ExecutorService executor;

    public static final float THUMBNAIL_DPI = 72f;
    public static final float PREVIEW_DPI = 300f;

    public static final float THUMBNAIL_SCALE = 1.0f; 
    public static final float PREVIEW_SCALE = 2.0f; 

    public PdfRenderSession(File file) throws IOException {
        this(Loader.loadPDF(file), true);
    }

    public PdfRenderSession(RandomAccessRead source) throws IOException {
        this(Loader.loadPDF(source), false);
    }

    private PdfRenderSession(PDDocument doc, boolean withExecutor) {
        this.document = doc;
        this.renderer = new PDFRenderer(document);
        if (withExecutor) {
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "page-render-session");
                t.setDaemon(true);
                return t;
            });
        } else {
            this.executor = null;
        }
    }

    public int getPageCount() {
        return document.getNumberOfPages();
    }
    
    public PDPage getPage(int index) {
        return document.getPage(index);
    }

    // --- Sync Rendering (DPI) ---

    public BufferedImage renderWithDPI(int pageIndex, float dpi) throws IOException {
        return renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
    }

    public byte[] renderToPngWithDPI(int pageIndex, float dpi) throws IOException {
        return imageToPng(renderWithDPI(pageIndex, dpi));
    }

    // --- Sync Rendering (Scale) ---

    public BufferedImage renderWithScale(int pageIndex, float scale) throws IOException {
        return renderer.renderImage(pageIndex, scale, ImageType.RGB);
    }

    public byte[] renderToPngWithScale(int pageIndex, float scale) throws IOException {
        return imageToPng(renderWithScale(pageIndex, scale));
    }

    private byte[] imageToPng(BufferedImage img) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        }
    }

    // --- Async Rendering (DPI) ---

    public CompletableFuture<byte[]> renderToPngWithDPIAsync(int pageIndex, float dpi) {
        return submitAsync(() -> renderToPngWithDPI(pageIndex, dpi));
    }

    // --- Async Rendering (Scale) ---

    public CompletableFuture<byte[]> renderToPngWithScaleAsync(int pageIndex, float scale) {
        return submitAsync(() -> renderToPngWithScale(pageIndex, scale));
    }
    
    // CompletableFuture Methods
    public CompletableFuture<BufferedImage> renderThumbnailAsync(int pageIndex) {
        return submitAsync(() -> renderWithScale(pageIndex, THUMBNAIL_SCALE));
    }

    public CompletableFuture<BufferedImage> renderPreviewAsync(int pageIndex) {
        return submitAsync(() -> renderWithScale(pageIndex, PREVIEW_SCALE));
    }

    // Legacy Callback Support
    public void renderThumbnailAsync(int pageIndex, Consumer<BufferedImage> callback) {
        renderThumbnailAsync(pageIndex).thenAccept(img -> {
            if (callback != null) callback.accept(img);
        });
    }

    public void renderPreviewAsync(int pageIndex, Consumer<BufferedImage> callback) {
        renderPreviewAsync(pageIndex).thenAccept(img -> {
            if (callback != null) callback.accept(img);
        });
    }

    private <T> CompletableFuture<T> submitAsync(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (executor == null || executor.isShutdown()) {
            future.completeExceptionally(new IllegalStateException("Async execution not enabled for this session"));
            return future;
        }
        executor.submit(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public void close() {
        if (executor != null) executor.shutdownNow();
        try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}