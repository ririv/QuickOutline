package com.ririv.quickoutline.view;

import com.google.gson.Gson;
import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.service.PdfSvgService;
import com.ririv.quickoutline.service.atomic.AtomicBlockService;
import com.ririv.quickoutline.service.atomic.DocumentBlock;
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
import javafx.scene.control.*;
import javafx.scene.input.*;
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
import java.util.List;
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
    // 修改泛型：输入 String (HTML)，输出 String (JSON)
    // 为什么输出 JSON？因为序列化可能会耗时，我们放在后台线程做
    private TrailingThrottlePreviewer<String, String> previewer;

    private ScheduledExecutorService contentPoller;

    // 每个 Controller 实例独享一个 Server，避免多 Tab 数据冲突
    private LocalWebServer webServer;

    JsBridge bridge = new JsBridge();

    @Inject
    private AtomicBlockService atomicBlockService; // 注入新服务



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

    private void performPaste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String text = clipboard.getString();
            if (text == null) return;

            // 转义，防止 JS 语法错误
            String safeText = text.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            // 调用 Vditor 的插入接口
            webEngine.executeScript("window.insertContent && window.insertContent('" + safeText + "')");
        }
    }

    /**
     * 执行复制逻辑 (JS 获取选区 -> Java 剪贴板)
     */
    private void performCopy() {
        // 获取网页中选中的文本
        Object selection = webEngine.executeScript("window.getSelection().toString()");
        if (selection != null) {
            String selectedText = selection.toString();
            if (!selectedText.isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedText);
                Clipboard.getSystemClipboard().setContent(content);
            }
        }
    }

    /**
     * 执行剪切逻辑 (复制 -> JS 删除)
     */
    private void performCut() {
        // 1. 先复制
        performCopy();
        // 2. 再调用 JS 删除选中内容 (Vditor 或是普通 contenteditable 都支持 execCommand delete)
        // 或者使用 Vditor 的 delete API，但最通用的是这个：
        webEngine.executeScript("document.execCommand('delete')");
    }

    /**
     * 1. 初始化预览节流器 (TrailingThrottlePreviewer)
     * 防止用户输入过快导致频繁生成 PDF
     */
    /**
     * 1. 初始化预览节流器 (Atomic Block 版)
     */
    private void setupPreviewer() {
        final int PREVIEW_THROTTLE_MS = 250;

        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> {
            eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR));
            log.error(msg);
        });

        this.previewer = new TrailingThrottlePreviewer<>(PREVIEW_THROTTLE_MS,
                json -> {
                    String baseUri = null;
                    try {
                        if (currentFileState.getSrcFile() != null && currentFileState.getSrcFile().getParent() != null) {
                            baseUri = currentFileState.getSrcFile().getParent().toUri().toString();
                        }
                    } catch (Exception ex) {
                        log.warn("Base URI error", ex);
                    }

                    // --- 【核心逻辑变更】 ---
                    // 1. 调用 AtomicService 处理 HTML -> List<DocumentBlock>

                    PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads = PayloadsJsonParser.parseJson(json);
                    List<DocumentBlock> blocks = atomicBlockService.processHtml(mdEditorContentPayloads.html(), baseUri);

                    // 2. 在后台线程序列化为 JSON，减轻 UI 线程负担
                    return new Gson().toJson(blocks);
                },
                this::updatePreviewUI, // 成功回调
                e -> onError.accept("Preview failed: " + e.getMessage())
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
//                 startContentPoller();
            } else if (newState == Worker.State.FAILED) {
                log.error("Editor WebView load failed");
            }
        });

        // 1. 【关键】禁用 WebView 自带的那个没用的右键菜单
        webView.setContextMenuEnabled(false);

        // 2. 创建我们自己的 JavaFX 右键菜单
        ContextMenu contextMenu = new ContextMenu();

        MenuItem cutItem = new MenuItem("剪切"); // 或从 bundle 获取本地化字符串
        MenuItem copyItem = new MenuItem("复制");
        MenuItem pasteItem = new MenuItem("粘贴");

        // 绑定动作
        cutItem.setOnAction(e -> performCut());
        copyItem.setOnAction(e -> performCopy());
        pasteItem.setOnAction(e -> performPaste());

        contextMenu.getItems().addAll(cutItem, copyItem, new SeparatorMenuItem(), pasteItem);

        // 3. 监听鼠标事件，手动弹出菜单
        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                // 优化：根据是否有选中文本，禁用/启用 复制和剪切
                boolean hasSelection = false;
                try {
                    String sel = (String) webEngine.executeScript("window.getSelection().toString()");
                    hasSelection = sel != null && !sel.isEmpty();
                } catch (Exception ignore) {}

                cutItem.setDisable(!hasSelection);
                copyItem.setDisable(!hasSelection);

                // 优化：根据剪贴板是否有内容，禁用/启用 粘贴
                pasteItem.setDisable(!Clipboard.getSystemClipboard().hasString());

                // 弹出菜单
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                // 点击左键隐藏菜单
                contextMenu.hide();
            }
        });

        // ... JSBridge 监听器保持不变 ...

        // 4. 键盘事件监听器 (复用逻辑)
        webView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isPasteShortcut(event)) {
                performPaste();
                event.consume();
            } else if (isCopyShortcut(event)) {
                performCopy();
                event.consume();
            }
            // Cut 的快捷键通常是 Ctrl+X
            else if (isCutShortcut(event)) {
                performCut();
                event.consume();
            }
        });
    }

    // 辅助方法：判断剪切快捷键
    private boolean isCutShortcut(KeyEvent event) {
        return (event.isShortcutDown() && event.getCode() == KeyCode.X);
    }

    // 辅助方法：判断粘贴快捷键 (兼容 Windows Ctrl+V 和 Mac Cmd+V)
    private boolean isPasteShortcut(KeyEvent event) {
        return (event.isShortcutDown() && event.getCode() == KeyCode.V);
    }

    // 辅助方法：判断复制快捷键
    private boolean isCopyShortcut(KeyEvent event) {
        return (event.isShortcutDown() && event.getCode() == KeyCode.C);
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
    /**
     * 更新预览 UI (Atomic Block 版)
     * 接收 JSON 字符串，推送到前端
     */
    private void updatePreviewUI(String blocksJson) {
        if (blocksJson == null || blocksJson.isEmpty()) return;

        Platform.runLater(() -> {
            try {
                if (previewWebEngine == null) return;

                String targetUrl = webServer.getBaseUrl() + "atomic_preview.html";
                String currentLoc = previewWebEngine.getLocation();

                // 定义更新动作
                Runnable doUpdate = () -> {
                    try {
                        JSObject window = (JSObject) previewWebEngine.executeScript("window");
                        // 调用前端的 renderAtomicBlocks
                        window.call("renderAtomicBlocks", blocksJson);
                    } catch (Exception e) {
                        log.error("JS Call failed", e);
                    }
                };

                // 判断是否需要加载页面
                if (currentLoc == null || !currentLoc.startsWith(targetUrl)) {
                    log.info("Loading Atomic Preview: {}", targetUrl);

                    // 一次性监听器
                    javafx.beans.value.ChangeListener<Worker.State> listener = new javafx.beans.value.ChangeListener<>() {
                        @Override
                        public void changed(javafx.beans.value.ObservableValue<? extends Worker.State> obs, Worker.State old, Worker.State state) {
                            if (state == Worker.State.SUCCEEDED) {
                                doUpdate.run();
                                previewWebEngine.getLoadWorker().stateProperty().removeListener(this);
                            }
                        }
                    };
                    previewWebEngine.getLoadWorker().stateProperty().addListener(listener);
                    previewWebEngine.load(targetUrl);
                } else {
                    // 页面已就绪，直接推送数据
                    doUpdate.run();
                }

            } catch (Exception e) {
                log.error("Failed to update UI", e);
            }
        });
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
                Platform.runLater(() -> {
                    eventBus.post(new ShowMessageEvent("An error occurred: " + e.getMessage(), Message.MessageType.ERROR));
                    e.printStackTrace();
                });
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