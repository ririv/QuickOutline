package com.ririv.quickoutline.pdfProcess;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages loading a PDF document and rendering its pages as images.
 */
public class PdfBatchRasterizer implements AutoCloseable {

    private final File sourceFile;
    private final PDDocument document;
    private final PDFRenderer renderer;
    private final Object renderLock = new Object();
    private static final float DEFAULT_DPI = 150; // Default DPI for rendering
    private volatile boolean closed = false;
    private volatile boolean subsamplingAllowed = true;

    /**
     * Constructs a PdfPreview instance for a given PDF file with default DPI.
     *
     * @param file The PDF file to be previewed.
     * @throws IOException if there is an error loading the PDF file.
     */
    public PdfBatchRasterizer(File file) throws IOException {
        this(file, DEFAULT_DPI);
    }

    /**
     * Constructs a PdfPreview instance for a given PDF file.
     *
     * @param file The PDF file to be previewed.
     * @param dpi The resolution (dots per inch) for rendering pages.
     * @throws IOException if there is an error loading the PDF file.
     */
    public PdfBatchRasterizer(File file, float dpi) throws IOException {
        this.sourceFile = file;
        this.document = Loader.loadPDF(file);
        this.renderer = new PDFRenderer(document);
        // Hint: allow subsampling to speed up rendering for large images
        this.renderer.setSubsamplingAllowed(true);
    }

    /**
     * Gets the total number of pages in the PDF document.
     *
     * @return The page count.
     */
    public int getPageCount() {
        ensureOpen();
        return document.getNumberOfPages();
    }

    private static final float THUMBNAIL_DPI = 72; // DPI for thumbnails
    private static final float PREVIEW_DPI = 300; // DPI for high-resolution previews

    /**
     * Renders a specific page of the PDF to a JavaFX Image and passes it to a callback.
     *
     * @param pageIndex The 0-based index of the page to render.
     * @param callback  The callback to be executed with the rendered Image.
     * @throws IOException if there is an error rendering the page.
     */
    public void renderImageWithCallback(int pageIndex, java.util.function.Consumer<BufferedImage> callback) throws IOException {
        renderImageWithCallback(pageIndex, DEFAULT_DPI, callback);
    }

    /**
     * Renders a thumbnail for a specific page and passes it to a callback.
     *
     * @param pageIndex The 0-based index of the page to render.
     * @param callback  The callback to be executed with the rendered Image.
     * @throws IOException if there is an error rendering the page.
     */
    public void renderThumbnail(int pageIndex, java.util.function.Consumer<BufferedImage> callback) throws IOException {
        renderImageWithCallback(pageIndex, THUMBNAIL_DPI, callback);
    }

    /**
     * Renders a high-resolution preview image for a specific page and passes it to a callback.
     *
     * @param pageIndex The 0-based index of the page to render.
     * @param callback  The callback to be executed with the rendered Image.
     * @throws IOException if there is an error rendering the page.
     */
    public void renderPreviewImage(int pageIndex, Consumer<BufferedImage> callback) throws IOException {
        renderImageWithCallback(pageIndex, PREVIEW_DPI, callback);
    }

    /**
     * Renders a specific page of the PDF to a JavaFX Image with a specific DPI and passes it to a callback.
     *
     * @param pageIndex The 0-based index of the page to render.
     * @param dpi The resolution (dots per inch) for rendering.
     * @param callback  The callback to be executed with the rendered Image.
     * @throws IOException if there is an error rendering the page.
     */
    public void renderImageWithCallback(int pageIndex, float dpi, Consumer<BufferedImage> callback) throws IOException {
        ensureOpen();
        if (pageIndex < 0 || pageIndex >= getPageCount()) {
            throw new IllegalArgumentException("Page index " + pageIndex + " is out of bounds.");
        }
        synchronized (renderLock) {
            renderer.setSubsamplingAllowed(subsamplingAllowed);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(pageIndex, dpi);
            if (callback != null) {
                callback.accept(bufferedImage);
            }
        }
    }

    public BufferedImage renderImage(int pageIndex, float dpi) throws IOException {
        ensureOpen();
        if (pageIndex < 0 || pageIndex >= getPageCount()) {
            throw new IllegalArgumentException("Page index " + pageIndex + " is out of bounds.");
        }
        synchronized (renderLock) {
            renderer.setSubsamplingAllowed(subsamplingAllowed);
            return renderer.renderImageWithDPI(pageIndex, dpi);
        }
    }


    /**
     * Closes the underlying PDDocument to release resources.
     * This method should be called when the preview is no longer needed.
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (document != null) {
            document.close();
        }
    }


    public List<BufferedImage> view() {

        // 1. 使用 iText 生成 PDF 到内存
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 添加内容到 PDF
        document.add(new Paragraph("Hello, this is a dynamically generated PDF!"));
        document.add(new Paragraph("This is a second line."));
        document.close();

        try {
            // 2. 使用 PDFBox 渲染 PDF 为图像
            PDDocument doc = Loader.loadPDF(outputStream.toByteArray());
            PDFRenderer renderer = new PDFRenderer(doc);
            // 渲染第一页为 BufferedImage
            BufferedImage bufferedImage = renderer.renderImage(0);
            doc.close();

            // 转换为 JavaFX Image
            ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", imageOutputStream);
            ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageOutputStream.toByteArray());

            BufferedImage image = ImageIO.read(imageInputStream);
            List<BufferedImage> imageList =  new ArrayList<>();
            imageList.add(image);
            return imageList;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Enable or disable subsampling to accelerate rendering at the cost of detail for downscaled images.
     */
    public void setSubsamplingAllowed(boolean allowed) {
        this.subsamplingAllowed = allowed;
    }

    /**
     * Batch render a page range. If parallelism > 1, this method safely renders in parallel by opening
     * independent PDDocument instances per worker to avoid thread-safety issues in PDFBox.
     *
     * Note: Intended for one-off, offline batch rendering scenarios. Not recommended for interactive UI rendering.
     *
     * @param fromPage inclusive, 0-based
     * @param toPageInclusive inclusive, 0-based
     * @param dpi resolution
     * @param parallelism number of worker threads; if <=1 renders sequentially using this instance
     * @param onPage callback for each page image
     * @throws IOException on I/O errors
     */
    public void batchRenderRange(int fromPage, int toPageInclusive, float dpi, int parallelism,
                                 java.util.function.BiConsumer<Integer, BufferedImage> onPage) throws IOException {
        ensureOpen();
        int pageCount = getPageCount();
        if (fromPage < 0 || toPageInclusive >= pageCount || fromPage > toPageInclusive) {
            throw new IllegalArgumentException("Invalid page range");
        }
        if (parallelism <= 1) {
            for (int i = fromPage; i <= toPageInclusive; i++) {
                BufferedImage img = renderImage(i, dpi);
                if (onPage != null) onPage.accept(i, img);
            }
            return;
        }

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(parallelism, r -> {
            Thread t = new Thread(r, "page-image-render-batch");
            t.setDaemon(true);
            return t;
        });

        java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger(fromPage);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

        for (int w = 0; w < parallelism; w++) {
            futures.add(pool.submit(() -> {
                try (PDDocument doc = Loader.loadPDF(sourceFile)) {
                    PDFRenderer localRenderer = new PDFRenderer(doc);
                    localRenderer.setSubsamplingAllowed(subsamplingAllowed);
                    while (true) {
                        int page = next.getAndIncrement();
                        if (page > toPageInclusive) break;
                        BufferedImage img = localRenderer.renderImageWithDPI(page, dpi);
                        if (onPage != null) onPage.accept(page, img);
                    }
                }
                return null;
            }));
        }

        // wait
        for (java.util.concurrent.Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.util.concurrent.ExecutionException e) {
                throw new IOException("Batch render failed", e.getCause());
            }
        }
        pool.shutdown();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("PageImageRender is closed");
        }
    }
}
