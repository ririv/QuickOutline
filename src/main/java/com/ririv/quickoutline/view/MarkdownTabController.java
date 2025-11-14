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
import javafx.scene.control.Label;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private TrailingThrottlePreviewer<String, byte[]> previewer;
    private ScheduledExecutorService contentPoller; // Java-side fallback poller

    @Inject
    public MarkdownTabController(CurrentFileState currentFileState, AppEventBus eventBus, MarkdownService markdownService) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
        this.markdownService = markdownService;
    }

    @FXML
    public void initialize() {
        System.out.println("[Controller] initialize start");
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

        // --- Debugging: Listen for JavaScript console messages ---
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("[Controller] WebView load succeeded; setting javaCallback");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaCallback", new JavaCallback());

                // Override console.log to pipe messages to the Java console
                webEngine.executeScript(
                    "console.log = function(message) {" +
                    "    javaCallback.log(message);" +
                    "};"
                );
                System.out.println("[Controller] console.log overridden");
                // 启动轮询：防止 JS 回调链路丢失时仍能捕捉内容变化
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
        System.out.println("[Controller] initialize complete");
    }

    private void updatePreviewUI(byte[] pdfBytes) {
        final int RENDER_DPI = 120; // 降低 DPI 减少阻塞
        System.out.println("[Preview DEBUG] updatePreviewUI request bytes=" + (pdfBytes == null ? 0 : pdfBytes.length));
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
                long renderCost = System.currentTimeMillis() - start;
                Platform.runLater(() -> {
                    previewVBox.getChildren().clear();
                    String ts = java.time.LocalTime.now().withNano(0).toString();
                    Label meta = new Label("Rendered " + ts + " pages=" + pageCount + " bytes=" + pdfBytes.length + " cost=" + renderCost + "ms");
                    meta.setStyle("-fx-font-size:11px; -fx-text-fill:#666; -fx-padding:2 4 4 4;");
                    previewVBox.getChildren().add(meta);
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
                e.printStackTrace();
            }
        }, "pdf-render-bg").start();
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
                            System.out.println("[Preview DEBUG] poll diff detected len=" + polled.length());
                            triggerPreviewIfChanged(polled);
                        }
                    } catch (Exception ex) {
                        System.out.println("[Preview DEBUG] poll error: " + ex.getMessage());
                    }
                });
            } catch (Exception ignore) {}
        }, 1200, 900, TimeUnit.MILLISECONDS);
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
        // 主动拉取：JS 端只发信号 notifyUpdated(seq)，这里再从 WebView 获取内容
        public void notifyUpdated(int seq) {
            Platform.runLater(() -> {
                try {
                    String newContent = (String) webEngine.executeScript("window.getContent()");
                    if (newContent == null) newContent = "";
                    System.out.println("[Preview DEBUG] notifyUpdated seq=" + seq + " len=" + newContent.length());
                    MarkdownTabController.this.triggerPreviewIfChanged(newContent);
                } catch (Exception e) {
                    System.out.println("[Preview DEBUG] notifyUpdated error: " + e.getMessage());
                }
            });
        }

        public void onContentChanged(String content) {
            Platform.runLater(() -> {
                final String newContent = (content == null) ? "" : content;
                System.out.println("[Preview DEBUG] JavaCallback.onContentChanged len=" + newContent.length());
                MarkdownTabController.this.triggerPreviewIfChanged(newContent);
            });
        }

        public void log(String message) {
            System.out.println("[WebView Console] " + message);
        }
    }

    private void triggerPreviewIfChanged(String newContent) {
        boolean identical = newContent.equals(markdownContent);
        markdownContent = newContent; // 更新缓存（调试期即使相同也更新，便于 poll 比较）
        System.out.println("[Preview DEBUG] throttle trigger len=" + newContent.length() + (identical?" (identical)":""));
        previewer.trigger(newContent); // 调试阶段：允许相同内容也触发，确认链路完整
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
            e.printStackTrace();
        }
    }
}
