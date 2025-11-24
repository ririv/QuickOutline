package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.service.pdfpreview.PdfImageService;
import com.ririv.quickoutline.service.pdfpreview.PdfSvgService;
import com.ririv.quickoutline.service.webserver.LocalWebServer;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import com.ririv.quickoutline.utils.PayloadsJsonParser;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.MarkdownImageHandler;
import com.ririv.quickoutline.view.utils.TrailingThrottlePreviewer;
import com.ririv.quickoutline.view.webview.WebViewEditorSupport;
import com.ririv.quickoutline.view.webview.JsBridge;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import com.ririv.quickoutline.model.SvgPageMetadata;

import java.io.FileOutputStream;
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
    private WebView webView; // 统一的 WebView (包含编辑器和预览)

    @FXML
    private TextField insertPosTextField;

    @FXML
    private Button previewButton;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final MarkdownService markdownService;

    private WebEngine webEngine;      // 统一引擎

    private String editorHtmlContent = ""; // 缓存前端内容，防止重复渲染
    // 修改泛型：输入 String (HTML)，输出 FastByteArrayOutputStream (零拷贝流)
    private TrailingThrottlePreviewer<String, FastByteArrayOutputStream> previewer;

    private ScheduledExecutorService contentPoller;

    JsBridge bridge = new JsBridge();


    @Inject
    private PdfSvgService pdfSvgService; // 注入服务

    @Inject
    private PdfImageService pdfImageService; // 记得加这个注入

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
        // 负责调度：Markdown -> HTML -> PDF Stream -> Update UI
        setupPreviewer();

        // 2. 配置 WebView (编辑器 + 预览)
        setupWebViewConfig();

        // 3. 配置输入框格式化
        setupInputFields();

        // 4. 【核心】启动本地服务器并加载页面
        // Server 必须在加载任何页面之前启动，以确保资源路径可达
        loadEditor();

        log.debug("Controller initialize complete");
    }

    /**
     * 1. 初始化预览节流器 (TrailingThrottlePreviewer)
     */
    private void setupPreviewer() {
        final int PREVIEW_THROTTLE_MS = 500; // 建议设大一点 (500ms)，给 iText 缓冲时间

        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        // 输入是 JSON 字符串，输出是 FastByteArrayOutputStream
        this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
                jsonString -> { // 【注意】这里接收到的是 JSON 字符串

                    // 1. 计算 Base URI (用于加载图片)
                    String baseUri = null;
                    try {
                        if (currentFileState.getSrcFile() != null && currentFileState.getSrcFile().getParent() != null) {
                            baseUri = currentFileState.getSrcFile().getParent().toUri().toString();
                        }
                    } catch (Exception ex) {
                        log.warn("[Preview] failed to compute baseUri", ex);
                    }

                    // 2. 【关键步骤】解析 JSON
                    PayloadsJsonParser.MdEditorContentPayloads payloads = PayloadsJsonParser.parseJson(jsonString);

                    // 3. 生成 PDF (耗时操作)
                    return markdownService.convertHtmlToPdfStream(payloads, baseUri, onMessage, onError);
                },
                this::updatePreviewUISvg, // 成功回调：调用 SVG Diff 更新逻辑
                e -> onError.accept("PDF preview failed: " + e.getMessage())
        );
    }

    /**
     * 2. 配置 WebView
     */
    private void setupWebViewConfig() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // 监听加载状态，注入 Java 对象 (JSBridge)
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                log.info("WebView load succeeded");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                window.setMember("debugBridge", new JsBridge.DebugBridge()); // 同时也注入 debugBridge

                // 如果需要轮询兜底，可在此处启动
//                 startContentPoller();
            } else if (newState == Worker.State.FAILED) {
                log.error("WebView load failed");
            }
        });

        // 安装编辑器增强功能（右键菜单、剪贴板快捷键）
        new WebViewEditorSupport(webView).install();
    }

    /**
     * 3. 配置输入框
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
     * 4. 加载编辑器资源 (含 Server 启动)
     */
    private void loadEditor() {
        String urlToLoad = null;

        // --- 策略 1: 本地 HTTP 服务器 (推荐) ---
        try {
            LocalWebServer server = LocalWebServer.getInstance();
            server.start("/web");
            urlToLoad = server.getBaseUrl() + "markdown-tab.html";
            log.info("[Load Strategy 1] Local WebServer started successfully. URL: {}", urlToLoad);
        } catch (Exception e) {
            log.error("[Load Strategy 1] Failed to start LocalWebServer. Trying fallback...", e);
        }

        // --- 策略 2: Classpath 直接加载 (IDE 兜底) ---
        if (urlToLoad == null) {
            URL resource = getClass().getResource("/web/markdown-tab.html");
            if (resource != null) {
                urlToLoad = resource.toExternalForm();
                log.warn("[Load Strategy 2] Falling back to direct Classpath loading. Note: Icons/MathJax might fail in jpackage environment. URL: {}", urlToLoad);
            }
        }

        if (urlToLoad != null) {
            log.info("WebView loading: {}", urlToLoad);
            webEngine.load(urlToLoad);
        } else {
            String errorMsg = "FATAL: Could not find 'markdown-tab.html' in resources!";
            log.error(errorMsg);
            eventBus.post(new ShowMessageEvent("Failed to load editor resources. Please check logs.", Message.MessageType.ERROR));
        }
    }

    private void updatePreviewUISvg(FastByteArrayOutputStream pdfStream) {
        if (pdfStream == null || pdfStream.size() == 0) return;

        // 后台计算 Diff
        new Thread(() -> {
            try {
                // 1. 计算 Diff (使用 Fast Stream)
                var updates = pdfSvgService.diffPdfToSvg(pdfStream);

                if (updates.isEmpty()) return; // 无变化，不打扰 UI

                // 2. 将 SVG 内容存入 Server，并准备元数据
                LocalWebServer server = LocalWebServer.getInstance();
                long version = System.currentTimeMillis(); // 简单版本号，强制刷新缓存

                var metadataList = new java.util.ArrayList<SvgPageMetadata>();

                for (var update : updates) {
                    // 存入 SVG 内容
                    server.putSvg(LocalWebServer.DEFAULT_DOC_ID, String.valueOf(update.pageIndex()), update.svgContent());
                    
                    metadataList.add(new SvgPageMetadata(
                        update.pageIndex(), 
                        update.totalPages(), 
                        update.widthPt(), 
                        update.heightPt(),
                        version
                    ));
                }

                // 3. 序列化元数据
                String jsonString = new com.google.gson.Gson().toJson(metadataList);

                // 4. 推送前端
                Platform.runLater(() -> {
                    if (webEngine == null) return;
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.call("updateSvgPages", jsonString);
                    } catch (Exception e) {
                        log.error("UI Update failed", e);
                    }
                });
            } catch (Exception e) {
                log.error("SVG Diff failed", e);
            }
        }).start();
    }

    
    public void dispose() {
        if (contentPoller != null && !contentPoller.isShutdown()) {
            contentPoller.shutdownNow();
        }
        if (webView != null) {
            webView.getEngine().load(null);
        }
    }


    // ================= Action Handlers =================

    @FXML
    void insertImageIntoMarkdown() {
        javafx.stage.Window owner = webView.getScene() != null ? webView.getScene().getWindow() : null;
        new MarkdownImageHandler(currentFileState, eventBus).insertImage(owner, webEngine);
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
                    Consumer<String> onError = msg -> Platform.runLater(() -> {
                        log.error(msg);
                        eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR));
                    });

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
                
                // 使用 FastStream 获取数据
                FastByteArrayOutputStream pdfStream = markdownService.convertHtmlToPdfStream(mdEditorContentPayloads, null, onMessage, onError);

                if (pdfStream == null || pdfStream.size() == 0) {
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
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                // 写入文件
                                fos.write(pdfStream.getBuffer(), 0, pdfStream.size());
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
                Platform.runLater(() -> {
                    eventBus.post(new ShowMessageEvent("An error occurred: " + e.getMessage(), Message.MessageType.ERROR));
                    e.printStackTrace();
                });
            }
        }, "save-new-pdf").start();
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
