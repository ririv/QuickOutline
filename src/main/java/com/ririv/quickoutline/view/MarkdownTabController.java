package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.DebouncedPreviewer;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class MarkdownTabController {

    @FXML
    private EditorTextArea markdownTextArea;
    @FXML
    private VBox previewVBox;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final MarkdownService markdownService;

    private DebouncedPreviewer<String, byte[]> previewer;
    private byte[] lastGeneratedPdfBytes;

    @Inject
    public MarkdownTabController(CurrentFileState currentFileState, AppEventBus eventBus, MarkdownService markdownService) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
        this.markdownService = markdownService;
    }

    @FXML
    public void initialize() {
        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        this.previewer = new DebouncedPreviewer<>(500,
                markdownText -> markdownService.convertMarkdownToPdfBytes(markdownText, onMessage, onError),
                this::updatePreviewUI,
                e -> onError.accept("PDF preview failed: " + e.getMessage()));

        markdownTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                previewer.trigger(newVal);
            } else {
                previewVBox.getChildren().clear();
                lastGeneratedPdfBytes = null;
            }
        });
    }

    private void updatePreviewUI(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            previewVBox.getChildren().clear();
            lastGeneratedPdfBytes = null;
            return;
        }
        this.lastGeneratedPdfBytes = pdfBytes;
        try (PDDocument pdDocument = Loader.loadPDF(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            previewVBox.getChildren().clear();
            for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, 150);
                ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(previewVBox.getWidth() > 20 ? previewVBox.getWidth() - 20 : 400);
                previewVBox.getChildren().add(imageView);
            }
        } catch (IOException e) {
            Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to render PDF preview: " + e.getMessage(), Message.MessageType.ERROR)));
            e.printStackTrace();
        }
    }

    @FXML
    void previewAction(ActionEvent event) {
        String text = markdownTextArea.getText();
        if (text != null && !text.isEmpty()) {
            previewer.trigger(text);
        }
    }

    @FXML
    void renderToPdfAction(ActionEvent event) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first. The output will be saved in the same directory.", Message.MessageType.WARNING));
            return;
        }
        if (lastGeneratedPdfBytes == null) {
            eventBus.post(new ShowMessageEvent("No PDF has been generated yet. Please type something or click preview first.", Message.MessageType.WARNING));
            return;
        }

        Path srcPath = currentFileState.getSrcFile();
        String destFileName = srcPath.getFileName().toString().replaceFirst("[.][^.]+$", "") + "_from_markdown.pdf";
        Path destPath = srcPath.getParent().resolve(destFileName);
        File destFile = destPath.toFile();

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(lastGeneratedPdfBytes);
            eventBus.post(new ShowMessageEvent("Successfully rendered to " + destFile.getAbsolutePath(), Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to save PDF: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
