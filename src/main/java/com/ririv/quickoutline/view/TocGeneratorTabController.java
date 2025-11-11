package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import jakarta.inject.Inject;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.controls.message.Message;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;

public class TocGeneratorTabController {

    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final CurrentFileState currentFileState;
    private final BookmarkSettingsState bookmarkSettingsState;
    private final AppEventBus eventBus;
    private final PdfOutlineService pdfOutlineService;
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    @FXML
    private EditorTextArea tocContentTextArea;
    @FXML
    private TextArea titleTextArea;
    @FXML
    private VBox previewVBox;

    @Inject
    public TocGeneratorTabController(PdfTocPageGeneratorService pdfTocPageGeneratorService, CurrentFileState currentFileState, BookmarkSettingsState bookmarkSettingsState, AppEventBus eventBus, PdfOutlineService pdfOutlineService) {
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.currentFileState = currentFileState;
        this.bookmarkSettingsState = bookmarkSettingsState;
        this.eventBus = eventBus;
        this.pdfOutlineService = pdfOutlineService;
    }

    @FXML
    public void initialize() {
        titleTextArea.setText("Table of Contents");
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            tocContentTextArea.setText(rootBookmark.toOutlineString());
        }
    }

    @FXML
    void previewTocPageAction(ActionEvent event) {
        String tocContent = tocContentTextArea.getText();
        if (tocContent == null || tocContent.isBlank()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        String title = titleTextArea.getText();
        if (title == null || title.isBlank()) {
            title = "Table of Contents"; // Default title
        }

        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(tocContent, Method.INDENT);
        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            pdfTocPageGeneratorService.createTocPagePreview(title, rootBookmark.getChildren(), baos);

            try (PDDocument document = Loader.loadPDF(baos.toByteArray())) {
                PDFRenderer renderer = new PDFRenderer(document);
                previewVBox.getChildren().clear(); // Clear previous preview
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    BufferedImage bufferedImage = renderer.renderImageWithDPI(i, 150); // Render page at 150 DPI
                    ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(previewVBox.getWidth() - 20); // Adjust width to fit VBox padding
                    previewVBox.getChildren().add(imageView);
                }
            }

        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to generate TOC preview: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }

    @FXML
    void generateTocPageAction(ActionEvent event) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        String tocContent = tocContentTextArea.getText();
        if (tocContent == null || tocContent.isBlank()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        String title = titleTextArea.getText();
        if (title == null || title.isBlank()) {
            title = "Table of Contents"; // Default title
        }

        String srcFile = currentFileState.getSrcFile().toString();
        String destFile = currentFileState.getDestFile().toString();
        
        // The user might have edited the text, so we need to parse it back into bookmarks.
        // We assume the "indent" method for parsing, as it's the most common for TOCs.
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(tocContent, Method.INDENT);

        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        try {
            pdfTocPageGeneratorService.createTocPage(srcFile, destFile, title, rootBookmark.getChildren());
            eventBus.post(new ShowMessageEvent(bundle.getString("alert.FileSavedAt") + destFile, Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to generate TOC page: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
