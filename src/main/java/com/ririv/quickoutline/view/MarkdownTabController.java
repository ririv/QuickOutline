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
import com.google.gson.Gson;

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

    // DTO for renderPdf request
    private record RenderPdfPayload(String html, String styles, int insertPos, String style) {}

    @FXML
    private WebView webView;

    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;
    private final MarkdownService markdownService;

    private WebEngine webEngine;      // 统一引擎

    private String editorHtmlContent = ""; // 缓存前端内容，防止重复渲染
    private TrailingThrottlePreviewer<String, FastByteArrayOutputStream> previewer;

    private ScheduledExecutorService contentPoller;

    JsBridge bridge = new JsBridge();
    private final Gson gson = new Gson();

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

        // 1. 初始化 PDF 预览生成器
        setupPreviewer();

        // 2. 配置 WebView
        setupWebViewConfig();

        // 3. 启动本地服务器并加载页面
        loadEditor();

        log.debug("Controller initialize complete");
    }

    private void setupPreviewer() {
        final int PREVIEW_THROTTLE_MS = 500; 

        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
                jsonString -> {
                    String baseUri = null;
                    try {
                        if (currentFileState.getSrcFile() != null && currentFileState.getSrcFile().getParent() != null) {
                            baseUri = currentFileState.getSrcFile().getParent().toUri().toString();
                        }
                    } catch (Exception ex) {
                        log.warn("[Preview] failed to compute baseUri", ex);
                    }

                    PayloadsJsonParser.MdEditorContentPayloads payloads = PayloadsJsonParser.parseJson(jsonString);
                    return markdownService.convertHtmlToPdfStream(payloads, baseUri, onMessage, onError);
                },
                this::updatePreviewUISvg,
                e -> onError.accept("PDF preview failed: " + e.getMessage())
        );
    }

    private void setupWebViewConfig() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        
        // Register handlers
        bridge.setRenderPdfHandler(this::handleRenderPdf);
        bridge.setUpdatePreviewHandler(this::handleUpdatePreview);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                log.info("WebView load succeeded");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                window.setMember("debugBridge", new JsBridge.DebugBridge()); 
            } else if (newState == Worker.State.FAILED) {
                log.error("WebView load failed");
            }
        });
    }

    private void loadEditor() {
        String urlToLoad = null;
        try {
            LocalWebServer server = LocalWebServer.getInstance();
            server.start("/web");
            urlToLoad = server.getBaseUrl() + "markdown-tab.html";
            log.info("[Load Strategy 1] Local WebServer started successfully. URL: {}", urlToLoad);
        } catch (Exception e) {
            log.error("[Load Strategy 1] Failed to start LocalWebServer. Trying fallback...", e);
        }

        if (urlToLoad == null) {
            URL resource = getClass().getResource("/web/markdown-tab.html");
            if (resource != null) {
                urlToLoad = resource.toExternalForm();
            }
        }

        if (urlToLoad != null) {
            webEngine.load(urlToLoad);
        } else {
            String errorMsg = "FATAL: Could not find 'markdown-tab.html' in resources!";
            log.error(errorMsg);
            eventBus.post(new ShowMessageEvent("Failed to load editor resources. Please check logs.", Message.MessageType.ERROR));
        }
    }

    private void updatePreviewUISvg(FastByteArrayOutputStream pdfStream) {
        if (pdfStream == null || pdfStream.size() == 0) return;

        new Thread(() -> {
            try {
                var updates = pdfSvgService.diffPdfToSvg(pdfStream);
                if (updates.isEmpty()) return; 

                LocalWebServer server = LocalWebServer.getInstance();
                long version = System.currentTimeMillis();

                var metadataList = new java.util.ArrayList<SvgPageMetadata>();

                for (var update : updates) {
                    server.putSvg(LocalWebServer.DEFAULT_DOC_ID, String.valueOf(update.pageIndex()), update.svgContent());
                    
                    metadataList.add(new SvgPageMetadata(
                        update.pageIndex(), 
                        update.totalPages(), 
                        update.widthPt(), 
                        update.heightPt(),
                        version
                    ));
                }

                String jsonString = gson.toJson(metadataList);

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

    // ================= Handlers =================

    @FXML
    void insertImageIntoMarkdown() {
        javafx.stage.Window owner = webView.getScene() != null ? webView.getScene().getWindow() : null;
        new MarkdownImageHandler(currentFileState, eventBus).insertImage(owner, webEngine);
    }

    private void handleUpdatePreview(String json) {
        // json contains {html, styles} (from MdEditor.getPayloads)
        previewer.trigger(json);
    }

    private void handleRenderPdf(String json) {
        try {
            RenderPdfPayload payload = gson.fromJson(json, RenderPdfPayload.class);
            renderToPdfAction(payload);
        } catch (Exception e) {
            log.error("Error handling render PDF request", e);
            eventBus.post(new ShowMessageEvent("Invalid data from editor", Message.MessageType.ERROR));
        }
    }

    private void renderToPdfAction(RenderPdfPayload payload) {
        if (currentFileState.getSrcFile() == null) {
            savePreviewAsNewPdf(payload);
            return;
        }
        
        new Thread(() -> {
            String currentHtml = payload.html();
            if (currentHtml == null || currentHtml.isBlank()) {
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("No HTML content to render.", Message.MessageType.WARNING)));
                return;
            }

            int insertPos = payload.insertPos() <= 0 ? 1 : payload.insertPos();

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

                // Adapt payload to MdEditorContentPayloads for service
                PayloadsJsonParser.MdEditorContentPayloads contentPayloads = 
                    new PayloadsJsonParser.MdEditorContentPayloads(payload.html(), payload.styles());

                markdownService.insertPageFromHtml(srcFile, destFile, contentPayloads, insertPos, baseUri, onMessage, onError);

                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Successfully rendered to " + destFile, Message.MessageType.SUCCESS)));
            } catch (IOException e) {
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to render to PDF: " + e.getMessage(), Message.MessageType.ERROR)));
                log.error("Render to PDF failed", e);
            }
        }, "pdf-render").start();
    }

    private void savePreviewAsNewPdf(RenderPdfPayload payload) {
        new Thread(() -> {
            if (payload.html() == null || payload.html().isBlank()) {
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Markdown content is empty.", Message.MessageType.WARNING)));
                return;
            }

            Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
            Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
            
            PayloadsJsonParser.MdEditorContentPayloads contentPayloads = 
                    new PayloadsJsonParser.MdEditorContentPayloads(payload.html(), payload.styles());

            FastByteArrayOutputStream pdfStream = markdownService.convertHtmlToPdfStream(contentPayloads, null, onMessage, onError);

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
