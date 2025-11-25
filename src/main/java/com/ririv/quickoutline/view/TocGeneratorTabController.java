package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.service.pdfpreview.PdfImageService;
import com.ririv.quickoutline.service.webserver.LocalWebServer;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import com.ririv.quickoutline.view.utils.DebouncedPreviewer;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import com.ririv.quickoutline.view.webview.JsBridge;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.ririv.quickoutline.model.SectionConfig; // Added import

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import com.ririv.quickoutline.model.TocPayload;

public class TocGeneratorTabController {

    private static final Logger log = LoggerFactory.getLogger(TocGeneratorTabController.class);

    private record TocPreviewInput(String tocContent, String title, PageLabelNumberingStyle style, SectionConfig header, SectionConfig footer) {}
    
    // Data transfer object from Frontend
    // private record TocPayload(String tocContent, String title, int offset, int insertPos, String style) {}

    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final CurrentFileState currentFileState;
    private final BookmarkSettingsState bookmarkSettingsState;
    private final AppEventBus eventBus;
    private final PdfOutlineService pdfOutlineService;
    private final PdfImageService pdfImageService;
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final Gson gson = new Gson();

    @FXML
    private WebView previewWebView;

    private DebouncedPreviewer<TocPreviewInput, FastByteArrayOutputStream> previewer;
    private WebEngine previewWebEngine;
    private JsBridge bridge = new JsBridge();

    @Inject
    public TocGeneratorTabController(PdfTocPageGeneratorService pdfTocPageGeneratorService, CurrentFileState currentFileState, BookmarkSettingsState bookmarkSettingsState, AppEventBus eventBus, PdfOutlineService pdfOutlineService, PdfImageService pdfImageService) {
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.currentFileState = currentFileState;
        this.bookmarkSettingsState = bookmarkSettingsState;
        this.eventBus = eventBus;
        this.pdfOutlineService = pdfOutlineService;
        this.pdfImageService = pdfImageService;
    }

    @FXML
    public void initialize() {
        setupDebouncedPreviewer();
        setupPreviewWebViewConfig();
        loadPreviewPage();
    }

    private void setupPreviewWebViewConfig() {
        if (previewWebView != null) {
            previewWebEngine = previewWebView.getEngine();
            previewWebEngine.setJavaScriptEnabled(true);
            previewWebView.setContextMenuEnabled(false);

            // Register handlers for frontend events
            bridge.setTocHandlers(this::handlePreviewToc, this::handleGenerateToc);

            previewWebEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) previewWebEngine.executeScript("window");
                    window.setMember("debugBridge", new JsBridge.DebugBridge());
                    window.setMember("javaBridge", bridge); // Inject bridge

                    log.info("TOC Preview page loaded successfully.");
                }
            });
        }
    }

    private void loadPreviewPage() {
        try {
            LocalWebServer server = LocalWebServer.getInstance();
            server.start("/web");
            String urlToLoad = server.getBaseUrl() + "toc-tab.html";
            log.info("Loading TOC Preview page: {}", urlToLoad);
            previewWebEngine.load(urlToLoad);
        } catch (Exception e) {
            log.error("Failed to start LocalWebServer for TOC preview", e);
        }
    }

    private void setupDebouncedPreviewer() {
        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        this.previewer = new DebouncedPreviewer<>(50,
                input -> generatePreviewBytes(input, onMessage, onError),
                this::updatePreviewUI,
                e -> onError.accept("TOC preview failed: " + e.getMessage()));
    }

    // Handler for 'previewToc' from JS
    private void handlePreviewToc(String json) {
        try {
            TocPayload payload = gson.fromJson(json, TocPayload.class);
            
            // Update settings state
            Platform.runLater(() -> {
                bookmarkSettingsState.setOffset(payload.offset());
            });

            String styleStr = payload.style() != null ? payload.style() : "None";
            PageLabelNumberingStyle style = PageLabel.STYLE_MAP.getOrDefault(styleStr, PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
            
            if (payload.tocContent() == null || payload.tocContent().isBlank()) {
                return;
            }
            
            String title = payload.title() == null || payload.title().isBlank() ? "Table of Contents" : payload.title();

            previewer.trigger(new TocPreviewInput(payload.tocContent(), title, style, payload.header(), payload.footer()));
        } catch (Exception e) {
            log.error("Error handling preview request from JS", e);
        }
    }

    // Handler for 'generateToc' from JS
    private void handleGenerateToc(String json) {
        try {
            TocPayload payload = gson.fromJson(json, TocPayload.class);
            generateTocPage(payload);
        } catch (Exception e) {
            log.error("Error handling generate request from JS", e);
            eventBus.post(new ShowMessageEvent("Invalid data from editor", Message.MessageType.ERROR));
        }
    }

    private FastByteArrayOutputStream generatePreviewBytes(TocPreviewInput input, Consumer<String> onMessage, Consumer<String> onError) {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(input.tocContent(), Method.INDENT);
        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            return null;
        }

        try (FastByteArrayOutputStream baos = new FastByteArrayOutputStream()) {
            pdfTocPageGeneratorService.createTocPagePreview(input.title(), input.style(), rootBookmark, baos, input.header(), input.footer(), onMessage, onError);
            return baos;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate TOC preview bytes", e);
        }
    }

    private void updatePreviewUI(FastByteArrayOutputStream pdfStream) {
        if (pdfStream == null || pdfStream.size() == 0) return;

        new Thread(() -> {
            try {
                var updates = pdfImageService.diffPdfToImages(pdfStream);
                if (updates.isEmpty()) return;

                LocalWebServer server = LocalWebServer.getInstance();
                for (var update : updates) {
                    String imageKey = update.pageIndex() + ".png";
                    byte[] imgData = pdfImageService.getImageData(update.pageIndex());
                    if (imgData != null) {
                        server.putImage(LocalWebServer.DEFAULT_DOC_ID, imageKey, imgData);
                    }
                }

                String jsonString = new com.google.gson.Gson().toJson(updates);

                Platform.runLater(() -> {
                    try {
                        if (previewWebEngine == null) return;
                        JSObject window = (JSObject) previewWebEngine.executeScript("window");
                        window.call("updateImagePages", jsonString);
                    } catch (Exception e) {
                        log.error("UI update failed", e);
                    }
                });
            } catch (Exception e) {
                log.error("Image generation failed", e);
            }
        }).start();
    }

    private void generateTocPage(TocPayload payload) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        if (payload.tocContent() == null || payload.tocContent().isBlank()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        String title = (payload.title() == null || payload.title().isBlank()) ? "Table of Contents" : payload.title();
        int insertPos = payload.insertPos() <= 0 ? 1 : payload.insertPos();
        
        String styleStr = payload.style() != null ? payload.style() : "None";
        PageLabelNumberingStyle style = PageLabel.STYLE_MAP.get(styleStr);

        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(payload.tocContent(), Method.INDENT);

        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        new Thread(() -> {
            try {
                String srcFile = currentFileState.getSrcFile().toString();
                String destFile = currentFileState.getDestFile().toString();
                Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
                Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
                
                pdfTocPageGeneratorService.createTocPage(srcFile, destFile, title, insertPos, style, rootBookmark, payload.header(), payload.footer(), onMessage, onError);
                
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent(bundle.getString("alert.FileSavedAt") + destFile, Message.MessageType.SUCCESS)));
            } catch (IOException e) {
                Platform.runLater(() -> eventBus.post(new ShowMessageEvent("Failed to generate TOC page: " + e.getMessage(), Message.MessageType.ERROR)));
                e.printStackTrace();
            }
        }).start();
    }

    public void dispose() {
        if (previewWebView != null) {
            previewWebView.getEngine().load(null);
        }
    }
}
