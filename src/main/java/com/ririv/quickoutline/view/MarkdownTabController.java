package com.ririv.quickoutline.view;

import com.google.gson.Gson;
import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.service.PdfSvgService;
import com.ririv.quickoutline.service.webserver.LocalWebServer;
import com.ririv.quickoutline.utils.PayloadsJsonParser;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.TrailingThrottlePreviewer;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private WebView webView; // 左侧：Markdown 编辑器 (Vditor)

    @FXML
    private WebView previewWebView; // 右侧：PDF 预览 (PDF.js)，替代了原来的 VBox

    @FXML
    private TextField insertPosTextField;

    @FXML
    private Button previewButton;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final MarkdownService markdownService;

    private WebEngine webEngine;      // 编辑器引擎
    private WebEngine previewWebEngine; // 预览引擎

    private String editorHtmlContent = ""; // 缓存前端内容，防止重复渲染
    private TrailingThrottlePreviewer<String, byte[]> previewer;
    private ScheduledExecutorService contentPoller;

    // 每个 Controller 实例独享一个 Server，避免多 Tab 数据冲突
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
        // 负责调度：Markdown -> HTML -> PDF byte[] -> Update UI
        setupPreviewer();

        // 2. 配置 编辑器 WebView (左侧)
        setupWebViewConfig();

        // 3. 配置 预览 WebView (右侧)
        // 【注意】必须初始化预览引擎，否则 updatePreviewUI 会报空指针
        setupPreviewWebViewConfig();

        // 4. 配置输入框格式化
        setupInputFields();

        // 5. 【核心】启动本地服务器并加载编辑器
        // Server 必须在加载任何页面之前启动，以确保资源路径可达
        loadEditor();

        log.debug("Controller initialize complete");
    }

    /**
     * 1. 初始化预览节流器 (TrailingThrottlePreviewer)
     * 防止用户输入过快导致频繁生成 PDF
     */
    private void setupPreviewer() {
        final int PREVIEW_THROTTLE_MS = 250; // 节流延迟

        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
                htmlContent -> {
                    String baseUri = null;
                    try {
                        // 动态获取当前 PDF 所在的父目录作为 Base URI (用于解析图片相对路径)
                        if (currentFileState.getSrcFile() != null && currentFileState.getSrcFile().getParent() != null) {
                            baseUri = currentFileState.getSrcFile().getParent().toUri().toString();
                        }
                        log.info("[Preview] dynamic baseUri={}", baseUri);
                    } catch (Exception ex) {
                        log.warn("[Preview] failed to compute baseUri", ex);
                    }

                    // 解析前端传来的 JSON
                    PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads = PayloadsJsonParser.parseJson(htmlContent);
                    // 生成 PDF 字节流 (耗时操作，在后台线程运行)
                    return markdownService.convertHtmlToPdfBytes(mdEditorContentPayloads, baseUri, onMessage, onError);
                },
                this::updatePreviewUI, // 成功回调：更新 PDF.js
                e -> onError.accept("PDF preview failed: " + e.getMessage()) // 失败回调
        );
    }

    /**
     * 2. 配置 编辑器 WebView
     */
    private void setupWebViewConfig() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // 监听加载状态，注入 Java 对象 (JSBridge)
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                log.info("Editor WebView load succeeded");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);

                // 如果需要轮询兜底，可在此处启动
                 startContentPoller();
            } else if (newState == Worker.State.FAILED) {
                log.error("Editor WebView load failed");
            }
        });
    }

    public class DebugBridge {
        public void log(String msg) { log.info("[SVG-JS] {}", msg); }
        public void error(String msg) { log.error("[SVG-JS Error] {}", msg); }
    }
    /**
     * 3. 配置 预览 WebView
     * 包含 CSS 注入(隐藏进度条) 和 JS 注入(保持滚动位置)
     */
    private void setupPreviewWebViewConfig() {
        if (previewWebView != null) {
            previewWebEngine = previewWebView.getEngine();
            previewWebEngine.setJavaScriptEnabled(true);
            previewWebView.setContextMenuEnabled(false);

            // 监听加载状态
            previewWebEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    // 注入调试桥
                    JSObject window = (JSObject) previewWebEngine.executeScript("window");
                    window.setMember("debugBridge", new DebugBridge());

                    log.info("Preview page loaded successfully.");
                }
            });
        }
    }
    /**
     * 4. 配置输入框
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
     * 5. 加载编辑器资源 (含 Server 启动)
     * 解决 jpackage 打包后资源路径协议 (jar:/jrt:) 不兼容问题
     */
    private void loadEditor() {
        String urlToLoad = null;

        // --- 策略 1: 本地 HTTP 服务器 (推荐) ---
        try {
            if (webServer == null) {
                webServer = new LocalWebServer();
            }
            // 启动服务器，映射 classpath:/web 目录
            // 直接读取 Jar 包内资源，无需解压
            webServer.start("/web");

            // 获取 http://127.0.0.1:端口/editor.html
            urlToLoad = webServer.getBaseUrl() + "editor.html";
            log.info("[Load Strategy 1] Local WebServer started successfully. URL: {}", urlToLoad);

        } catch (Exception e) {
            log.error("[Load Strategy 1] Failed to start LocalWebServer. Trying fallback...", e);
            // 确保清理资源
            if (webServer != null) {
                webServer.stop();
                webServer = null;
            }
        }

        // --- 策略 2: Classpath 直接加载 (IDE 兜底) ---
        if (urlToLoad == null) {
            URL resource = getClass().getResource("/web/editor.html");
            if (resource != null) {
                urlToLoad = resource.toExternalForm();
                log.warn("[Load Strategy 2] Falling back to direct Classpath loading. Note: Icons/MathJax might fail in jpackage environment. URL: {}", urlToLoad);
            }
        }

        if (urlToLoad != null) {
            log.info("WebView loading: {}", urlToLoad);
            webEngine.load(urlToLoad);
        } else {
            String errorMsg = "FATAL: Could not find 'editor.html' in resources!";
            log.error(errorMsg);
            eventBus.post(new ShowMessageEvent("Failed to load editor resources. Please check logs.", Message.MessageType.ERROR));
        }
    }

    /**
     * 【核心修改】更新预览界面
     * 使用 WebView + PDF.js 替代原本的 Image 预览，性能更好，且支持文字选择
     */
    @Inject
    private PdfSvgService pdfSvgService; // 注入服务
    private void updatePreviewUI(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) return;

        // 后台计算 Diff
        new Thread(() -> {
            try {
                // 1. 生成 Diff
                var updates = pdfSvgService.diffPdfToSvg(pdfBytes);
                if (updates.isEmpty()) return;

                // 2. 序列化 JSON
                // 确保你使用了 Gson 或者 Jackson
                String jsonString = new com.google.gson.Gson().toJson(updates);

                // 3. UI 线程更新
                Platform.runLater(() -> {
                    try {
                        if (previewWebEngine == null) return;

                        String svgPageUrl = webServer.getBaseUrl() + "svg_preview.html";
                        String currentUrl = previewWebEngine.getLocation();

                        // 定义更新任务
                        Runnable performUpdate = () -> {
                            try {
                                JSObject window = (JSObject) previewWebEngine.executeScript("window");
                                // 【核心】使用 call 方法直接传递 JSON 字符串，避免转义导致的 SyntaxError
                                window.call("updateSvgPages", jsonString);
                            } catch (Exception e) {
                                log.error("Failed to call updateSvgPages", e);
                            }
                        };

                        // 判断是否需要加载页面
                        if (currentUrl == null || !currentUrl.startsWith(svgPageUrl)) {
                            log.info("Loading SVG preview page...");

                            // 添加一次性监听器，等待页面加载完再执行更新
                            previewWebEngine.getLoadWorker().stateProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                                @Override
                                public void changed(javafx.beans.value.ObservableValue<? extends Worker.State> obs, Worker.State old, Worker.State state) {
                                    if (state == Worker.State.SUCCEEDED) {
                                        performUpdate.run();
                                        previewWebEngine.getLoadWorker().stateProperty().removeListener(this);
                                    }
                                }
                            });

                            previewWebEngine.load(svgPageUrl);
                        } else {
                            // 页面已就绪，直接更新
                            performUpdate.run();
                        }
                    } catch (Exception e) {
                        log.error("Error in UI update thread", e);
                    }
                });
            } catch (Exception e) {
                log.error("SVG generation failed", e);
            }
        }).start();
    }

    /**
     * 【重要】资源释放
     * 必须在 App 退出或 Tab 关闭时调用，释放端口
     */
    public void dispose() {
        // 1. 关闭 WebServer (释放端口)
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }

        // 2. 关闭轮询线程
        if (contentPoller != null && !contentPoller.isShutdown()) {
            contentPoller.shutdownNow();
        }

        // 3. 清理 WebView (防止内存泄漏)
        if (webView != null) {
            webView.getEngine().load(null);
        }
        if (previewWebView != null) {
            previewWebView.getEngine().load(null);
        }
    }


    // ================= Action Handlers =================

    @FXML
    void insertImageIntoMarkdown() {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent("Please select a source PDF file first.", Message.MessageType.WARNING));
            return;
        }

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.svg")
        );

        // 【修复】使用 webView 获取当前窗口句柄 (之前使用的是 previewVBox)
        javafx.stage.Window owner = webView.getScene() != null ? webView.getScene().getWindow() : null;
        java.io.File file = chooser.showOpenDialog(owner);

        if (file == null) return;

        try {
            Path chosen = file.toPath();
            Path finalFile = ensureUnderPdfImages(chosen);
            String relPath = relativizeToPdfDir(finalFile);
            // 路径转义 (Markdown 偏好正斜杠)
            String escaped = relPath.replace("\\", "/").replace("'", "\\'");
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

    @FXML
    void renderToPdfAction() {
        if (currentFileState.getSrcFile() == null) {
            savePreviewAsNewPdf();
            return;
        }
        new Thread(() -> {
            try {
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
                    if (insertPos <= 0) insertPos = 1;
                } catch (NumberFormatException e) { }

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
                log.error("Render action failed", e);
            }
        }, "pdf-render").start();
    }

    // ================= Helpers =================

    private void savePreviewAsNewPdf() {
        new Thread(() -> {
            try {
                String json = bridge.getContentSync(webEngine);
                PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads = PayloadsJsonParser.parseJson(json);
                if (mdEditorContentPayloads.html() == null || mdEditorContentPayloads.html().isBlank()) {
                    Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Markdown content is empty.", Message.MessageType.WARNING)));
                    return;
                }

                Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
                Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
                byte[] pdfBytes = markdownService.convertHtmlToPdfBytes(mdEditorContentPayloads, null, onMessage, onError);

                if (pdfBytes == null || pdfBytes.length == 0) {
                    Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to generate PDF from Markdown.", Message.MessageType.ERROR)));
                    return;
                }

                Platform.runLater(() -> {
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Save New PDF");
                    fileChooser.getExtensionFilters().add(
                            new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                    );
                    fileChooser.setInitialFileName("Untitled.pdf");
                    java.io.File file = fileChooser.showSaveDialog(webView.getScene().getWindow());

                    if (file != null) {
                        new Thread(() -> {
                            try {
                                Files.write(file.toPath(), pdfBytes);
                                Platform.runLater(() -> {
                                    eventBus.post(new ShowMessageEvent("Successfully saved new PDF to " + file.getAbsolutePath(), Message.MessageType.SUCCESS));
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

    private String relativizeToPdfDir(Path file) {
        Path src = currentFileState.getSrcFile();
        if (src == null) return file.getFileName().toString();
        Path pdfDir = src.getParent();
        try {
            String rel = pdfDir.relativize(file).toString();
            return rel.replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return file.getFileName().toString();
        }
    }

    private Path ensureUnderPdfImages(Path chosenFile) throws IOException {
        Path src = currentFileState.getSrcFile();
        if (src == null) return chosenFile;
        Path pdfDir = src.getParent();
        Path normalizedPdfDir = pdfDir.toAbsolutePath().normalize();
        Path normalizedChosen = chosenFile.toAbsolutePath().normalize();
        if (normalizedChosen.startsWith(normalizedPdfDir)) {
            return normalizedChosen;
        }
        Path imagesDir = normalizedPdfDir.resolve("images");
        Files.createDirectories(imagesDir);
        Path dest = imagesDir.resolve(chosenFile.getFileName());
        Files.copy(chosenFile, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
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
                String json = bridge.getContentSync(webEngine);
                triggerPreviewIfChanged(json);
            } catch (Exception e) { /* ignore */ }
        }, 1200, 1000, TimeUnit.MILLISECONDS);
    }

    private void triggerPreviewIfChanged(String newContent) {
        if (newContent == null) newContent = "";
        if (newContent.equals(editorHtmlContent)) return;
        editorHtmlContent = newContent;
        previewer.trigger(newContent);
    }
}