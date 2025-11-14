package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.TrailingThrottlePreviewer;
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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class MarkdownTabController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MarkdownTabController.class);

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
    private TrailingThrottlePreviewer<String, byte[]> previewer;
    private ScheduledExecutorService contentPoller;

    @Inject
    public MarkdownTabController(CurrentFileState currentFileState, AppEventBus eventBus, MarkdownService markdownService) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
        this.markdownService = markdownService;
    }

    @FXML
    public void initialize() {
    log.debug("Controller initialize start");
    final int PREVIEW_THROTTLE_MS = 250; // 节流延迟：在持续输入下确保周期性更新
        // --- Previewer Setup ---
        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
    // 使用 TrailingThrottlePreviewer：确保在输入高频时仍能落地周期性渲染，并在执行期间检测到内容再次变化时追加一次尾随运行
    this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
        markdownText -> markdownService.convertMarkdownToPdfBytes(markdownText, onMessage, onError),
        this::updatePreviewUI,
        e -> onError.accept("PDF preview failed: " + e.getMessage()));

        // --- WebView Setup ---
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        final URL url = getClass().getResource("/web/editor.html");

        // --- WebView ready ---
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                log.info("WebView load succeeded");

                // 启动轻量轮询作为兜底，确保回调偶发丢失时仍能抓到变更
                startContentPoller();
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
    log.debug("Controller initialize complete");
    }

    private void startContentPoller() {
        if (contentPoller != null) return;
        contentPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "markdown-content-poller");
            t.setDaemon(true);
            return t;
        });
        contentPoller.scheduleAtFixedRate(() -> {
            try {
                Platform.runLater(() -> {
                    try {
                        String polled = (String) webEngine.executeScript("window.getContent && window.getContent()");
                        if (polled == null) polled = "";
                        if (!polled.equals(markdownContent)) {
                            if (log.isDebugEnabled()) log.debug("poll diff detected len={}", polled.length());
                            triggerPreviewIfChanged(polled);
                        }
                    } catch (Exception ex) {
                        // swallow
                    }
                });
            } catch (Exception ignore) {}
        }, 1200, 1000, TimeUnit.MILLISECONDS);
    }

    private void updatePreviewUI(byte[] pdfBytes) {
    final int RENDER_DPI = 120; // 降低 DPI 减少阻塞
    if (log.isDebugEnabled()) log.debug("updatePreviewUI bytes={}", (pdfBytes == null ? 0 : pdfBytes.length));
        if (pdfBytes == null || pdfBytes.length == 0) {
            previewVBox.getChildren().clear();
            return;
        }
        // 将耗时 PDF 处理移到后台线程，避免阻塞 FX Thread 导致后续输入事件丢失
        new Thread(() -> {
            long start = System.currentTimeMillis();
            try (PDDocument pdDocument = Loader.loadPDF(pdfBytes)) {
                PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
                int pageCount = pdDocument.getNumberOfPages();
                BufferedImage[] pages = new BufferedImage[pageCount];
                for (int i = 0; i < pageCount; i++) {
                    pages[i] = pdfRenderer.renderImageWithDPI(i, RENDER_DPI);
                }
                Platform.runLater(() -> {
                    previewVBox.getChildren().clear();
                    double fitWidth = previewVBox.getWidth() > 20 ? previewVBox.getWidth() - 20 : 400;
                    for (BufferedImage bufferedImage : pages) {
                        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                        imageView.setPreserveRatio(true);
                        imageView.setFitWidth(fitWidth);
                        previewVBox.getChildren().add(imageView);
                    }
                    // 渲染结束后重新聚焦编辑器，确保继续输入触发 change
                    try { webEngine.executeScript("window.editorView && window.editorView.focus()"); } catch(Exception ignore) {}
                });
            } catch (IOException e) {
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to render PDF preview: " + e.getMessage(), Message.MessageType.ERROR)));
                log.error("Render PDF preview failed", e);
            }
        }, "pdf-render-bg").start();
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

    // 不再使用 JavaCallback：仅依赖 Java 侧轮询

    private void triggerPreviewIfChanged(String newContent) {
        if (newContent.equals(markdownContent)) {
            // unchanged: skip redundant render
            return;
        }
        markdownContent = newContent;
    if (log.isDebugEnabled()) log.debug("throttle trigger len={}", newContent.length());
        previewer.trigger(newContent);
        if (newContent.isEmpty()) {
            previewVBox.getChildren().clear();
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
            log.error("Render to PDF failed", e);
        }
    }
}
