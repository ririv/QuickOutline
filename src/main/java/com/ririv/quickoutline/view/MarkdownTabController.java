package com.ririv.quickoutline.view;

import com.ririv.quickoutline.view.controls.EditorTextArea;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.itextpdf.html2pdf.HtmlConverter;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollBar;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings("removal")
public class MarkdownTabController {

    @FXML
    private EditorTextArea markdownTextArea;
    @FXML
    private WebView previewWebView;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    private boolean isSyncing = false;
    private ScrollBar editorScrollBar;

    @Inject
    public MarkdownTabController(CurrentFileState currentFileState, AppEventBus eventBus) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
    }

    @FXML
    public void initialize() {
        markdownTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            Node document = parser.parse(newValue);
            String htmlContent = renderer.render(document);
            previewWebView.getEngine().loadContent(htmlContent);
        });

        // Wait for nodes to be rendered
        Platform.runLater(() -> {
            editorScrollBar = (ScrollBar) markdownTextArea.lookup(".scroll-bar:vertical");
            if (editorScrollBar != null) {
                // Sync from editor to web view
                editorScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (isSyncing) return;
                    isSyncing = true;

                    double editorScrollMax = editorScrollBar.getMax();
                    double editorScrollMin = editorScrollBar.getMin();
                    double editorScrollValue = newVal.doubleValue();

                    if (editorScrollMax <= editorScrollMin) {
                        isSyncing = false;
                        return;
                    }

                    double scrollPercentage = (editorScrollValue - editorScrollMin) / (editorScrollMax - editorScrollMin);

                    Object result = previewWebView.getEngine().executeScript("document.body.scrollHeight - window.innerHeight");
                    if (result instanceof Number) {
                        double webViewScrollHeight = ((Number) result).doubleValue();
                        previewWebView.getEngine().executeScript("window.scrollTo(0, " + (webViewScrollHeight * scrollPercentage) + ")");
                    }
                    isSyncing = false;
                });
            }
        });

        // Sync from web view to editor
        previewWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) previewWebView.getEngine().executeScript("window");
                window.setMember("javaBridge", new JSBridge());
                previewWebView.getEngine().executeScript(
                        "window.onscroll = function() {" +
                        "    var scrollHeight = document.body.scrollHeight - window.innerHeight;" +
                        "    if (scrollHeight > 0) {" +
                        "        var percentage = window.scrollY / scrollHeight;" +
                        "        javaBridge.setScroll(percentage);" +
                        "    }" +
                        "}"
                );
            }
        });
    }

    public class JSBridge {
        public void setScroll(double percentage) {
            if (isSyncing) return;
            isSyncing = true;
            if (editorScrollBar != null) {
                double editorScrollMax = editorScrollBar.getMax();
                double editorScrollMin = editorScrollBar.getMin();
                editorScrollBar.setValue(editorScrollMin + (editorScrollMax - editorScrollMin) * percentage);
            }
            isSyncing = false;
        }
    }

    @FXML
    void renderToPdfAction(ActionEvent event) {
        String markdownText = markdownTextArea.getText();
        if (markdownText == null || markdownText.isBlank()) {
            eventBus.post(new ShowMessageEvent("Markdown content is empty.", Message.MessageType.WARNING));
            return;
        }

        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first. The output will be saved in the same directory.", Message.MessageType.WARNING));
            return;
        }

        // 1. Parse Markdown to HTML
        Node document = parser.parse(markdownText);
        String htmlContent = renderer.render(document);

        // 2. Convert HTML to PDF
        Path srcPath = currentFileState.getSrcFile();
        String destFileName = srcPath.getFileName().toString().replaceFirst("[.][^.]+$", "") + "_from_markdown.pdf";
        Path destPath = srcPath.getParent().resolve(destFileName);
        File destFile = destPath.toFile();

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            HtmlConverter.convertToPdf(htmlContent, fos);
            eventBus.post(new ShowMessageEvent("Successfully rendered to " + destFile.getAbsolutePath(), Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to render PDF: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
