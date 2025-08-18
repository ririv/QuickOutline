package com.ririv.quickoutline.pdfProcess;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
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

/**
 * Manages loading a PDF document and rendering its pages as images.
 */
public class PdfPreview implements AutoCloseable {

    private final PDDocument document;
    private final PDFRenderer renderer;
    private static final float DEFAULT_DPI = 150; // Default DPI for rendering

    /**
     * Constructs a PdfPreview instance for a given PDF file with default DPI.
     *
     * @param file The PDF file to be previewed.
     * @throws IOException if there is an error loading the PDF file.
     */
    public PdfPreview(File file) throws IOException {
        this(file, DEFAULT_DPI);
    }

    /**
     * Constructs a PdfPreview instance for a given PDF file.
     *
     * @param file The PDF file to be previewed.
     * @param dpi The resolution (dots per inch) for rendering pages.
     * @throws IOException if there is an error loading the PDF file.
     */
    public PdfPreview(File file, float dpi) throws IOException {
        this.document = Loader.loadPDF(file);
        this.renderer = new PDFRenderer(document);
    }

    /**
     * Gets the total number of pages in the PDF document.
     *
     * @return The page count.
     */
    public int getPageCount() {
        return document.getNumberOfPages();
    }

    /**
     * Renders a specific page of the PDF to a JavaFX Image.
     *
     * @param pageIndex The 0-based index of the page to render.
     * @return A JavaFX Image of the rendered page.
     * @throws IOException if there is an error rendering the page.
     */
    public Image renderPage(int pageIndex) throws IOException {
        return renderPage(pageIndex, DEFAULT_DPI);
    }

    /**
     * Renders a specific page of the PDF to a JavaFX Image with a specific DPI.
     *
     * @param pageIndex The 0-based index of the page to render.
     * @param dpi The resolution (dots per inch) for rendering.
     * @return A JavaFX Image of the rendered page.
     * @throws IOException if there is an error rendering the page.
     */
    public Image renderPage(int pageIndex, float dpi) throws IOException {
        if (pageIndex < 0 || pageIndex >= getPageCount()) {
            throw new IllegalArgumentException("Page index " + pageIndex + " is out of bounds.");
        }
        BufferedImage bufferedImage = renderer.renderImageWithDPI(pageIndex, dpi);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }


    /**
     * Closes the underlying PDDocument to release resources.
     * This method should be called when the preview is no longer needed.
     */
    @Override
    public void close() throws IOException {
        if (document != null) {
            document.close();
        }
    }


    public List<Image> view() {

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

            Image image = new Image(imageInputStream);
            List<Image> imageList =  new ArrayList<>();
            imageList.add(image);
            return imageList;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}