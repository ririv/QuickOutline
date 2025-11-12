package com.ririv.quickoutline.view;

import com.itextpdf.html2pdf.HtmlConverter;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import jakarta.inject.Inject;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class MarkdownTabController {

    @FXML
    private EditorTextArea markdownTextArea;
    @FXML
    private VBox previewVBox;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    @Inject
    public MarkdownTabController(CurrentFileState currentFileState, AppEventBus eventBus) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
    }

    @FXML
    public void initialize() {
        // Initialization logic can be added here if needed in the future.
    }

    /**
     * Generates the PDF in memory and updates the preview area.
     * @return The byte array of the generated PDF, or null if generation fails.
     */
    private byte[] updatePreview() {
        String markdownText = markdownTextArea.getText();
        if (markdownText == null || markdownText.isBlank()) {
            eventBus.post(new ShowMessageEvent("Markdown content is empty.", Message.MessageType.WARNING));
            return null;
        }

        Node document = parser.parse(markdownText);
        String htmlContent = renderer.render(document);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Convert HTML to PDF in memory
            HtmlConverter.convertToPdf(htmlContent, baos);
            byte[] pdfBytes = baos.toByteArray();

            // Render the in-memory PDF for preview
            try (PDDocument pdDocument = Loader.loadPDF(pdfBytes)) {
                PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
                previewVBox.getChildren().clear(); // Clear previous preview
                for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
                    BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, 150); // 150 DPI for preview
                    ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(previewVBox.getWidth() > 20 ? previewVBox.getWidth() - 20 : 400); // Adjust width
                    previewVBox.getChildren().add(imageView);
                }
            }
            return pdfBytes;

        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to generate PDF preview: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    void previewAction(ActionEvent event) {
        updatePreview();
    }

    @FXML
    void renderToPdfAction(ActionEvent event) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first. The output will be saved in the same directory.", Message.MessageType.WARNING));
            return;
        }

        // Generate PDF and update preview first
        byte[] pdfBytes = updatePreview();

        if (pdfBytes == null) {
            // Preview generation failed, so don't save.
            return;
        }

        // Save the generated PDF bytes to a file
        Path srcPath = currentFileState.getSrcFile();
        String destFileName = srcPath.getFileName().toString().replaceFirst("[.][^.]+$", "") + "_from_markdown.pdf";
        Path destPath = srcPath.getParent().resolve(destFileName);
        File destFile = destPath.toFile();

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(pdfBytes);
            eventBus.post(new ShowMessageEvent("Successfully rendered to " + destFile.getAbsolutePath(), Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to save PDF: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
