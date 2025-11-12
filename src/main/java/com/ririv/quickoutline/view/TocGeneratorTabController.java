package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.view.utils.DebouncedPreviewer;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.controls.select.StyledSelect;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class TocGeneratorTabController {

    // Record to hold all inputs required for a preview
    private record TocPreviewInput(String tocContent, String title, PageLabelNumberingStyle style) {}

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
    @FXML
    private TextField offsetTF;
    @FXML
    private TextField insertPosTextField;
    @FXML
    private StyledSelect<String> numberingStyleComboBox;

    private DebouncedPreviewer<TocPreviewInput, byte[]> previewer;

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
        setupBookmarkBindings();
        setupInputFormatters();
        setupDebouncedPreviewer();
    }

    private void setupDebouncedPreviewer() {
        this.previewer = new DebouncedPreviewer<>(500, this::generatePreviewBytes, this::updatePreviewUI,
                e -> eventBus.post(new ShowMessageEvent("TOC preview failed: " + e.getMessage(), Message.MessageType.ERROR)));

        // Add listeners to all controls that affect the preview
        tocContentTextArea.textProperty().addListener((obs, ov, nv) -> triggerPreview());
        titleTextArea.textProperty().addListener((obs, ov, nv) -> triggerPreview());
        numberingStyleComboBox.valueProperty().addListener((obs, ov, nv) -> triggerPreview());
    }

    private void triggerPreview() {
        String tocContent = tocContentTextArea.getText();
        String title = titleTextArea.getText();
        PageLabelNumberingStyle style = PageLabel.STYLE_MAP.get(numberingStyleComboBox.getValue());

        if (tocContent == null || tocContent.isBlank()) {
            previewVBox.getChildren().clear();
            return;
        }
        if (title == null || title.isBlank()) {
            title = "Table of Contents"; // Default title
        }
        previewer.trigger(new TocPreviewInput(tocContent, title, style));
    }

    private byte[] generatePreviewBytes(TocPreviewInput input) {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(input.tocContent(), Method.INDENT);
        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            Platform.runLater(() -> eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING)));
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            pdfTocPageGeneratorService.createTocPagePreview(input.title(), input.style(), rootBookmark.getChildren(), baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate TOC preview bytes", e);
        }
    }

    private void updatePreviewUI(byte[] pdfBytes) {
        if (pdfBytes == null) return;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            previewVBox.getChildren().clear();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = renderer.renderImageWithDPI(i, 150);
                ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(previewVBox.getWidth() > 20 ? previewVBox.getWidth() - 20 : 400);
                previewVBox.getChildren().add(imageView);
            }
        } catch (IOException e) {
            Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to render TOC preview: " + e.getMessage(), Message.MessageType.ERROR)));
            e.printStackTrace();
        }
    }

    @FXML
    void previewTocPageAction(ActionEvent event) {
        triggerPreview(); // The button now also uses the debouncer logic
    }

    private void setupBookmarkBindings() {
        titleTextArea.setText("Table of Contents");
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            tocContentTextArea.setText(rootBookmark.toOutlineString());
        }

        bookmarkSettingsState.offsetProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.toString().equals(offsetTF.getText())) {
                offsetTF.setText(newVal.toString());
            }
        });

        offsetTF.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                bookmarkSettingsState.setOffset(Integer.parseInt(newVal));
            } catch (NumberFormatException e) {
                if (!Objects.equals(newVal, "-")) {
                    bookmarkSettingsState.setOffset(0);
                }
            }
        });
    }

    private void setupInputFormatters() {
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?[0-9]*")) {
                return change;
            }
            return null;
        };
        offsetTF.setTextFormatter(new TextFormatter<>(integerFilter));
        insertPosTextField.setTextFormatter(new TextFormatter<>(integerFilter));
        insertPosTextField.setText("1");

        numberingStyleComboBox.setItems(FXCollections.observableArrayList(PageLabel.STYLE_MAP.keySet()));
        numberingStyleComboBox.setValue("None");
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
            title = "Table of Contents";
        }

        int insertPos = 1;
        try {
            insertPos = Integer.parseInt(insertPosTextField.getText());
            if (insertPos <= 0) insertPos = 1;
        } catch (NumberFormatException e) {
            // Keep default
        }

        PageLabelNumberingStyle style = PageLabel.STYLE_MAP.get(numberingStyleComboBox.getValue());
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(tocContent, Method.INDENT);

        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        try {
            String srcFile = currentFileState.getSrcFile().toString();
            String destFile = currentFileState.getDestFile().toString();
            pdfTocPageGeneratorService.createTocPage(srcFile, destFile, title, insertPos, style, rootBookmark.getChildren());
            eventBus.post(new ShowMessageEvent(bundle.getString("alert.FileSavedAt") + destFile, Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to generate TOC page: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
