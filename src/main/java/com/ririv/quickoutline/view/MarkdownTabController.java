package com.ririv.quickoutline.view;

import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.utils.PayloadsJsonParser;
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
import netscape.javascript.JSObject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    @FXML
    private Button previewButton;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final MarkdownService markdownService;

    private WebEngine webEngine;
    private String editorHtmlContent = ""; // 前端内容（现为 HTML）
    private TrailingThrottlePreviewer<String, byte[]> previewer;
    private ScheduledExecutorService contentPoller;

    JsBridge bridge = new JsBridge();

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

        // 使用 TrailingThrottlePreviewer：每次执行时动态读取当前 srcFile，确保 baseUri 始终与当前源 PDF 目录一致
        this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
            htmlContent -> {
                String baseUri = null;
                try {
                    if (currentFileState.getSrcFile() != null && currentFileState.getSrcFile().getParent() != null) {
                        baseUri = currentFileState.getSrcFile().getParent().toUri().toString();
                    }
                    log.info("[Preview] dynamic baseUri={}", baseUri);
                } catch (Exception ex) {
                    log.warn("[Preview] failed to compute baseUri", ex);
                }
                PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads = PayloadsJsonParser.parseJson(htmlContent);
                return markdownService.convertHtmlToPdfBytes(mdEditorContentPayloads, baseUri, onMessage, onError);
            },
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

                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge); // "javaBridge" 是 JS 中的名字

                // 启动轻量轮询作为兜底，确保回调偶发丢失时仍能抓到变更
                startContentPoller();
            }
        });

        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            log.error("Failed to load editor.html resource");
        }

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
                log.debug("[JavaFX] 正在后台线程上同步请求 HTML...");

                // 这是“同步”调用！
                // 这个线程会在这里被阻塞，直到 JS 回调
                String json = bridge.getContentSync(webEngine);
                log.debug("[JavaFX] 成功同步获取到 HTML！}");
                // 成功！现在我们拿到了 html
                triggerPreviewIfChanged(json);

            } catch (Exception e) {
                // 处理超时或 JS 错误
                log.error("在同步等待时出错: " + e.getMessage());
                Platform.runLater(() -> {
                    // myLoadingSpinner.setVisible(false);
                    // showError("错误：" + e.getMessage());
                });
            }
        }, 1200, 1000, TimeUnit.MILLISECONDS);
    }

    private void updatePreviewUI(byte[] pdfBytes) {
        final int RENDER_DPI = 120; // 降低 DPI 减少阻塞
        log.debug("updatePreviewUI bytes={}", (pdfBytes == null ? 0 : pdfBytes.length));
        if (pdfBytes == null || pdfBytes.length == 0) {
            previewVBox.getChildren().clear();
            return;
        }
        // 将耗时 PDF 处理移到后台线程，避免阻塞 FX Thread 导致后续输入事件丢失
        new Thread(() -> {
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

    // 不再使用 JavaCallback：仅依赖 Java 侧轮询

    private void triggerPreviewIfChanged(String newContent) {
        // 成功！现在我们拿到了 html
        if (newContent == null) newContent = "";
        if (newContent.equals(editorHtmlContent)) {
            // unchanged: skip redundant render
            return;
        }
        editorHtmlContent = newContent;
        log.debug("throttle trigger len={}", newContent.length());
        previewer.trigger(newContent);
        if (newContent.isEmpty()) {
            previewVBox.getChildren().clear();
        }
    }

    // ================= Image helpers =================

    /**
     * 计算某个文件相对于当前源 PDF 目录的相对路径。
     */
    private String relativizeToPdfDir(Path file) {
        Path src = currentFileState.getSrcFile();
        if (src == null) return file.getFileName().toString();
        Path pdfDir = src.getParent();
        try {
            String rel = pdfDir.relativize(file).toString();
            return rel.replace('\\', '/');
        } catch (IllegalArgumentException e) {
            // 不同盘符等情况，退回仅文件名
            return file.getFileName().toString();
        }
    }

    /**
     * 确保文件位于 PDF 目录或其 images 子目录下；
     * 若不在，则复制到 pdfDir/images 下并返回目标路径。
     */
    private Path ensureUnderPdfImages(Path chosenFile) throws IOException {
        Path src = currentFileState.getSrcFile();
        if (src == null) return chosenFile;
        Path pdfDir = src.getParent();
        Path normalizedPdfDir = pdfDir.toAbsolutePath().normalize();
        Path normalizedChosen = chosenFile.toAbsolutePath().normalize();
        if (normalizedChosen.startsWith(normalizedPdfDir)) {
            return normalizedChosen; // 已在 PDF 目录树下
        }
        Path imagesDir = normalizedPdfDir.resolve("images");
        Files.createDirectories(imagesDir);
        Path dest = imagesDir.resolve(chosenFile.getFileName());
        Files.copy(chosenFile, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }


    @FXML
    void renderToPdfAction() {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first.", Message.MessageType.WARNING));
            return;
        }
        new Thread(() -> {

        try {
            // Get the latest HTML content directly from the editor for maximum accuracy
            String json = bridge.getContentSync(webEngine);
            PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads = PayloadsJsonParser.parseJson(json);

            String currentHtml = mdEditorContentPayloads.html();
            if (currentHtml == null || currentHtml.isBlank()) {
                eventBus.post(new ShowMessageEvent("No HTML content to render.", Message.MessageType.WARNING));
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
                String baseUri = null;
                if (currentFileState.getSrcFile() != null && currentFileState.getSrcFile().getParent() != null) {
                    baseUri = currentFileState.getSrcFile().getParent().toUri().toString();
                }
                Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
                Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

                markdownService.insertPageFromHtml(srcFile, destFile, mdEditorContentPayloads, insertPos, baseUri, onMessage, onError);

                eventBus.post(new ShowMessageEvent("Successfully rendered to " + destFile, Message.MessageType.SUCCESS));
            } catch (IOException e) {
                eventBus.post(new ShowMessageEvent("Failed to render to PDF: " + e.getMessage(), Message.MessageType.ERROR));
                log.error("Render to PDF failed", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        }, "pdf-render").start();

    }

        // ============ Image insert entrypoint (for future wiring) ============
    /**
     * 选择一张图片，必要时复制到 PDF 目录的 images/ 下，并在编辑器中插入相对路径的 Markdown 图像语法。
     * 注意：当前方法只提供核心逻辑骨架，尚未与具体菜单/按钮绑定。
     */
    @FXML
    void insertImageIntoMarkdown() {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first.", Message.MessageType.WARNING));
            return;
        }
        // 仅示意：使用简单的 AWT FileDialog 或 JavaFX FileChooser，具体实现可按现有 UI 规范调整
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("选择图片");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.svg")
        );
        javafx.stage.Window owner = previewVBox.getScene() != null ? previewVBox.getScene().getWindow() : null;
        java.io.File file = chooser.showOpenDialog(owner);
        if (file == null) return;
        try {
            Path chosen = file.toPath();
            Path finalFile = ensureUnderPdfImages(chosen);
            String relPath = relativizeToPdfDir(finalFile);
            String escaped = relPath.replace("\\", "\\\\").replace("'", "\\'");
            String js = "window.insertImageMarkdown('" + escaped + "')";
            webEngine.executeScript(js);
        } catch (IOException e) {
            log.error("Insert image into markdown failed", e);
            eventBus.post(new ShowMessageEvent("Failed to insert image: " + e.getMessage(), Message.MessageType.ERROR));
        }
    }


    @FXML
    void manualPreviewAction(ActionEvent event) {
        new Thread(() -> {
            try {
                String json = bridge.getContentSync(webEngine);
                triggerPreviewIfChanged(json);
            } catch (Exception e) {
                log.error("Manual preview failed: " + e.getMessage());
            }
        }).start();
    }
}
