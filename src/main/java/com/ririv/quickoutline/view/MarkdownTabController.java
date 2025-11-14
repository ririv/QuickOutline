package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.DebouncedPreviewer;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class MarkdownTabController {

    @FXML
    private WebView webView;
    @FXML
    private VBox previewVBox;
    @FXML
    private TextField insertPosTextField;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final MarkdownService markdownService;

    private WebEngine webEngine;
    private String markdownContent = "";
    private DebouncedPreviewer<String, byte[]> previewer;

    @Inject
    public MarkdownTabController(CurrentFileState currentFileState, AppEventBus eventBus, MarkdownService markdownService) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
        this.markdownService = markdownService;
    }

    @FXML
    public void initialize() {
        // --- Previewer Setup ---
        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
        this.previewer = new DebouncedPreviewer<>(500,
                markdownText -> markdownService.convertMarkdownToPdfBytes(markdownText, onMessage, onError),
                this::updatePreviewUI,
                e -> onError.accept("PDF preview failed: " + e.getMessage()));

        // --- WebView Setup ---
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        final URL url = getClass().getResource("/web/editor.html");

        // --- Debugging: Listen for JavaScript console messages ---
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaCallback", new JavaCallback());

                // Override console.log to pipe messages to the Java console
                webEngine.executeScript(
                    "console.log = function(message) {" +
                    "    javaCallback.log(message);" +
                    "};"
                );
            }
        });

        webEngine.load(url.toExternalForm());

        // --- Input Field Setup ---
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[0-9]*")) {
                return change;
            }
            return null;
        };
        insertPosTextField.setTextFormatter(new TextFormatter<>(integerFilter));
        insertPosTextField.setText("1");
    }

    private void updatePreviewUI(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            previewVBox.getChildren().clear();
            return;
        }
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

    /**
     * Sets the content of the Markdown editor.
     * @param content The new markdown content.
     */
    public void setMarkdown(String content) {
        Platform.runLater(() -> {
            if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                // Escape the content to be safely passed into a JavaScript string
                String escapedContent = content.replace("\\", "\\\\")
                                               .replace("'", "\\'")
                                               .replace("\n", "\\n")
                                               .replace("\r", "\\r");
                webEngine.executeScript("window.setContent('" + escapedContent + "')");
            }
        });
    }

    public class JavaCallback {
        public void onContentChanged(String content) {
            Platform.runLater(() -> {
                markdownContent = content; // Keep for debounced preview
                if (content != null && !content.isEmpty()) {
                    previewer.trigger(content);
                } else {
                    previewVBox.getChildren().clear();
                }
            });
        }

        public void log(String message) {
            System.out.println("[WebView Console] " + message);
        }
    }

    @FXML
    void renderToPdfAction() {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first.", Message.MessageType.WARNING));
            return;
        }

        // Get the latest content directly from the editor for maximum accuracy
        String currentMarkdown = (String) webEngine.executeScript("window.getContent()");

        if (currentMarkdown == null || currentMarkdown.isBlank()) {
            eventBus.post(new ShowMessageEvent("No Markdown content to render.", Message.MessageType.WARNING));
            return;
        }

        int insertPos = 1;
        try {
            insertPos = Integer.parseInt(insertPosTextField.getText());
            if (insertPos <= 0) {
                insertPos = 1;
            }
        } catch (NumberFormatException e) {
            // Keep default value of 1
        }

        try {
            String srcFile = currentFileState.getSrcFile().toString();
            String destFile = currentFileState.getDestFile().toString();
            Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
            Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

            markdownService.createMarkdownPage(srcFile, destFile, currentMarkdown, insertPos, onMessage, onError);

            eventBus.post(new ShowMessageEvent("Successfully rendered to " + destFile, Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to render to PDF: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
