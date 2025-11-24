package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.service.pdfpreview.PdfImageService;
import com.ririv.quickoutline.service.webserver.LocalWebServer;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.view.utils.DebouncedPreviewer;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.controls.select.StyledSelect;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import com.ririv.quickoutline.view.webview.JsBridge;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class TocGeneratorTabController {

    private static final Logger log = LoggerFactory.getLogger(TocGeneratorTabController.class);

    private record TocPreviewInput(String tocContent, String title, PageLabelNumberingStyle style) {}

    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final CurrentFileState currentFileState;
    private final BookmarkSettingsState bookmarkSettingsState;
    private final AppEventBus eventBus;
    private final PdfOutlineService pdfOutlineService;
    private final PdfImageService pdfImageService;
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    @FXML
    private EditorTextArea tocContentTextArea;
    @FXML
    private TextField titleText;
    @FXML
    private WebView previewWebView;
    @FXML
    private TextField offsetTF;
    @FXML
    private TextField insertPosTextField;
    @FXML
    private StyledSelect<String> numberingStyleComboBox;

    private DebouncedPreviewer<TocPreviewInput, byte[]> previewer;
    private WebEngine previewWebEngine;
    private LocalWebServer webServer;

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
        setupBookmarkBindings();
        setupInputFormatters();
        setupDebouncedPreviewer();
        setupPreviewWebViewConfig();
        loadPreviewPage();
    }

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
                    window.setMember("debugBridge", new JsBridge.DebugBridge());

                    log.info("TOC Preview page loaded successfully.");
                }
            });
        }
    }


    private void loadPreviewPage() {
        try {
            if (webServer == null) {
                webServer = new LocalWebServer();
            }
            webServer.start("/web");
            String urlToLoad = webServer.getBaseUrl() + "toc-tab.html";
            log.info("Loading TOC Preview page: {}", urlToLoad);
            previewWebEngine.load(urlToLoad);
        } catch (Exception e) {
            log.error("Failed to start LocalWebServer for TOC preview", e);
        }
    }

    private void setupDebouncedPreviewer() {
        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        this.previewer = new DebouncedPreviewer<>(500,
                input -> generatePreviewBytes(input, onMessage, onError),
                this::updatePreviewUI,
                e -> onError.accept("TOC preview failed: " + e.getMessage()));

        tocContentTextArea.textProperty().addListener((obs, ov, nv) -> triggerPreview());
        titleText.textProperty().addListener((obs, ov, nv) -> triggerPreview());
        numberingStyleComboBox.valueProperty().addListener((obs, ov, nv) -> triggerPreview());
    }

    private void triggerPreview() {
        String tocContent = tocContentTextArea.getText();
        String title = titleText.getText();
        PageLabelNumberingStyle style = PageLabel.STYLE_MAP.get(numberingStyleComboBox.getValue());

        if (tocContent == null || tocContent.isBlank()) {
            // Clear preview? Maybe send empty bytes or handle in updatePreviewUI
            return;
        }
        if (title == null || title.isBlank()) {
            title = "Table of Contents";
        }
        previewer.trigger(new TocPreviewInput(tocContent, title, style));
    }

    private byte[] generatePreviewBytes(TocPreviewInput input, Consumer<String> onMessage, Consumer<String> onError) {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(input.tocContent(), Method.INDENT);
        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            Platform.runLater(() -> eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING)));
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            pdfTocPageGeneratorService.createTocPagePreview(input.title(), input.style(), rootBookmark, baos, onMessage, onError);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate TOC preview bytes", e);
        }
    }

    private void updatePreviewUI(byte[] pdfBytes) {
        if (pdfBytes == null) return;

        new Thread(() -> {
            try {
                var updates = pdfImageService.diffPdfToImages(pdfBytes);
                if (updates.isEmpty()) return;

                for (var update : updates) {
                    String imageKey = update.pageIndex() + ".png";
                    byte[] imgData = pdfImageService.getImageData(update.pageIndex());
                    if (imgData != null) {
                        LocalWebServer.putImage(imageKey, imgData);
                    }
                }

                String jsonString = new com.google.gson.Gson().toJson(updates);

                Platform.runLater(() -> {
                    try {
                        if (previewWebEngine == null) return;
                        String previewUrl = webServer.getBaseUrl() + "toc-tab.html";
                        String currentLoc = previewWebEngine.getLocation();

                        Runnable doUpdate = () -> {
                            try {
                                JSObject window = (JSObject) previewWebEngine.executeScript("window");
                                window.call("updateImagePages", jsonString);
                            } catch (Exception e) {
                                log.error("JS update failed", e);
                            }
                        };

                        if (currentLoc == null || !currentLoc.startsWith(previewUrl)) {
                            previewWebEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                                if (state == Worker.State.SUCCEEDED) {
                                    doUpdate.run();
                                }
                            });
                            previewWebEngine.load(previewUrl);
                        } else {
                            doUpdate.run();
                        }
                    } catch (Exception e) {
                        log.error("UI update failed", e);
                    }
                });
            } catch (Exception e) {
                log.error("Image generation failed", e);
            }
        }).start();
    }

    public void dispose() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        if (previewWebView != null) {
            previewWebView.getEngine().load(null);
        }
    }

    @FXML
    void previewTocPageAction(ActionEvent event) {
        triggerPreview();
    }

    private void setupBookmarkBindings() {
        titleText.setText("Table of Contents");
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            tocContentTextArea.setText(rootBookmark.toOutlineString());
        }

        bookmarkSettingsState.offsetProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.toString().equals(offsetTF.getText())) {
                offsetTF.setText(newVal.toString());
            }
        });

        offsetTF.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                bookmarkSettingsState.setOffset(Integer.parseInt(newVal));
            } catch (NumberFormatException e) {
                if (!Objects.equals(newVal, "-")) {
                    bookmarkSettingsState.setOffset(0);
                }
            }
        });
    }

    private void setupInputFormatters() {
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?[0-9]*")) {
                return change;
            }
            return null;
        };
        offsetTF.setTextFormatter(new TextFormatter<>(integerFilter));
        insertPosTextField.setTextFormatter(new TextFormatter<>(integerFilter));
        insertPosTextField.setText("1");

        numberingStyleComboBox.setItems(FXCollections.observableArrayList(PageLabel.STYLE_MAP.keySet()));
        numberingStyleComboBox.setValue("None");
    }

    @FXML
    void generateTocPageAction(ActionEvent event) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        String tocContent = tocContentTextArea.getText();
        if (tocContent == null || tocContent.isBlank()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        String title = titleText.getText();
        if (title == null || title.isBlank()) {
            title = "Table of Contents";
        }

        int insertPos = 1;
        try {
            insertPos = Integer.parseInt(insertPosTextField.getText());
            if (insertPos <= 0) insertPos = 1;
        } catch (NumberFormatException e) {
            // Keep default
        }

        PageLabelNumberingStyle style = PageLabel.STYLE_MAP.get(numberingStyleComboBox.getValue());
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(tocContent, Method.INDENT);

        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        try {
            String srcFile = currentFileState.getSrcFile().toString();
            String destFile = currentFileState.getDestFile().toString();
            Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
            Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));
            pdfTocPageGeneratorService.createTocPage(srcFile, destFile, title, insertPos, style, rootBookmark, onMessage, onError);
            eventBus.post(new ShowMessageEvent(bundle.getString("alert.FileSavedAt") + destFile, Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to generate TOC page: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
