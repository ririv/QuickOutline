package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.webserver.LocalWebServer;
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

    private LocalWebServer webServer;

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

        // 1. 初始化 PDF 预览生成器 (Previewer)
        setupPreviewer();

        // 2. 配置 WebView 的基础设置和监听器 (JSBridge)
        setupWebViewConfig();

        // 3. 配置输入框格式化
        setupInputFields();

        // 4. 【核心】启动本地服务器并加载编辑器
        loadEditor();

        log.debug("Controller initialize complete");
    }

    /**
     * 1. 初始化预览节流器 (TrailingThrottlePreviewer)
     * 负责处理 Markdown -> HTML -> PDF 的转换逻辑
     */
    private void setupPreviewer() {
        final int PREVIEW_THROTTLE_MS = 250; // 节流延迟：在持续输入下确保周期性更新

        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        // 使用 TrailingThrottlePreviewer：每次执行时动态读取当前 srcFile，确保 baseUri 始终与当前源 PDF 目录一致
        this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
                htmlContent -> {
                    String baseUri = null;
                    try {
                        // 动态获取当前 PDF 所在的父目录作为 Base URI
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
                this::updatePreviewUI, // 成功回调
                e -> onError.accept("PDF preview failed: " + e.getMessage()) // 失败回调
        );
    }

    /**
     * 2. 配置 WebView 引擎和状态监听
     * 负责注入 JSBridge
     */
    private void setupWebViewConfig() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // 监听加载状态，注入 Java 对象
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                log.info("WebView load succeeded");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);

                // 启动轻量轮询作为兜底，确保回调偶发丢失时仍能抓到变更
//                startContentPoller();
            } else if (newState == Worker.State.FAILED) {
                log.error("WebView load failed");
            }
        });
    }

    /**
     * 3. 配置输入框
     * 限制只能输入数字
     */
    private void setupInputFields() {
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

    /**
     * 加载编辑器 HTML 资源。
     * <p>
     * 采用降级策略以兼容不同的运行环境：
     * 1. 优先使用 LocalWebServer (HTTP协议): 完美解决 jpackage 打包后 WebView 对 jar/jrt 协议及相对路径支持不佳的问题。
     * 2. 降级使用 Classpath 加载 (file/jar协议): 用于开发环境 (IDE) 或服务器启动失败时的兜底。
     */
    private void loadEditor() {
        String urlToLoad = null;

        // ---------------------------------------------------------
        // 策略 1 (首选): 启动本地 HTTP 服务器
        // ---------------------------------------------------------
        try {
            // 懒加载服务器实例
            if (webServer == null) {
                webServer = new LocalWebServer();
            }

            // 启动服务器，映射 Classpath 下的 /web 目录
            // 这一步在 jar 包环境中也能完美工作，直接读取 jar 内资源流
            webServer.start("/web");

            // 获取本地回环地址，例如 http://127.0.0.1:54321/editor.html
            urlToLoad = webServer.getBaseUrl() + "editor.html";
            log.info("[Load Strategy 1] Local WebServer started successfully. URL: {}", urlToLoad);

        } catch (Exception e) {
            log.error("[Load Strategy 1] Failed to start LocalWebServer. Trying fallback...", e);
            // 如果启动失败（极罕见，如端口耗尽），确保停止以释放资源
            if (webServer != null) {
                webServer.stop();
            }
        }

        // ---------------------------------------------------------
        // 策略 2 (兜底): 直接从 Classpath 加载
        // ---------------------------------------------------------
        if (urlToLoad == null) {
            // 这种情况通常发生在 IDE 开发环境中，或者服务器启动意外失败
            URL resource = getClass().getResource("/web/editor.html");
            if (resource != null) {
                urlToLoad = resource.toExternalForm();
                log.warn("[Load Strategy 2] Falling back to direct Classpath loading. " +
                        "Note: Icons/MathJax might fail in jpackage environment. URL: {}", urlToLoad);
            }
        }

        // ---------------------------------------------------------
        // 执行加载
        // ---------------------------------------------------------
        if (urlToLoad != null) {
            log.info("WebView loading: {}", urlToLoad);
            webEngine.load(urlToLoad);
        } else {
            // 致命错误：找不到资源文件
            String errorMsg = "FATAL: Could not find 'editor.html' in resources!";
            log.error(errorMsg);
            eventBus.post(new ShowMessageEvent("Failed to load editor resources. Please check logs.", Message.MessageType.ERROR));
        }
    }

    // 【重要】请确保在 Controller 销毁或 Tab 关闭时调用此方法
    public void dispose() {

        // 1. 关闭 WebServer
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }

        // 2. 关闭轮询线程 (如果有)
        if (contentPoller != null && !contentPoller.isShutdown()) {
            contentPoller.shutdownNow();
        }

        // 3. 清理 WebView (可选，防止内存泄漏)
        if (webView != null) {
            webView.getEngine().load(null);
        }
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


    private void savePreviewAsNewPdf() {
        new Thread(() -> {
            try {
                // 1. Get latest content from editor
                String json = bridge.getContentSync(webEngine);
                PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads = PayloadsJsonParser.parseJson(json);
                if (mdEditorContentPayloads.html() == null || mdEditorContentPayloads.html().isBlank()) {
                    Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Markdown content is empty.", Message.MessageType.WARNING)));
                    return;
                }

                // 2. Convert HTML to PDF bytes
                Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
                Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
                byte[] pdfBytes = markdownService.convertHtmlToPdfBytes(mdEditorContentPayloads, null, onMessage, onError); // baseUri is null

                if (pdfBytes == null || pdfBytes.length == 0) {
                    Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to generate PDF from Markdown.", Message.MessageType.ERROR)));
                    return;
                }

                // 3. Show FileChooser to get save path
                Platform.runLater(() -> {
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Save New PDF");
                    fileChooser.getExtensionFilters().add(
                            new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                    );
                    fileChooser.setInitialFileName("Untitled.pdf");
                    java.io.File file = fileChooser.showSaveDialog(webView.getScene().getWindow());

                    if (file != null) {
                        // 4. Save the file in a background thread
                        new Thread(() -> {
                            try {
                                Files.write(file.toPath(), pdfBytes);
                                Platform.runLater(() -> {
                                    eventBus.post(new ShowMessageEvent("Successfully saved new PDF to " + file.getAbsolutePath(), Message.MessageType.SUCCESS));
                                    // TODO: Optionally, post an event to open this new file
                                    // eventBus.post(new OpenFileEvent(file));
                                });
                            } catch (IOException e) {
                                log.error("Failed to save new PDF file", e);
                                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to save file: " + e.getMessage(), Message.MessageType.ERROR)));
                            }
                        }, "save-new-pdf-io").start();
                    }
                });

            } catch (Exception e) {
                log.error("Failed to save preview as new PDF", e);
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("An error occurred: " + e.getMessage(), Message.MessageType.ERROR)));
            }
        }, "save-new-pdf").start();
    }


    @FXML
    void renderToPdfAction() {
        if (currentFileState.getSrcFile() == null) {
            savePreviewAsNewPdf();
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
