package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.service.PdfImageService;
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
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

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
    private TrailingThrottlePreviewer<String, byte[]> previewer;

    private ScheduledExecutorService contentPoller;

    // 每个 Controller 实例独享一个 Server，避免多 Tab 数据冲突
    private LocalWebServer webServer;

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
    /**
     * 1. 初始化预览节流器
     * 负责调度：Markdown (前端JSON) -> 解析 -> PDF byte[] -> Update UI
     */
    private void setupPreviewer() {
        final int PREVIEW_THROTTLE_MS = 500; // 建议设大一点 (500ms)，给 iText 缓冲时间

        Consumer<String> onMessage = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.INFO)));
        Consumer<String> onError = msg -> Platform.runLater(() -> eventBus.post(new ShowMessageEvent(msg, Message.MessageType.ERROR)));

        // <String, byte[]> : 输入是 JSON 字符串，输出是 PDF 字节数组
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
                    // 前端传回的数据结构: { "html": "...", "styles": "..." }
                    // 必须解析成对象，否则 iText 会把 JSON 括号当成文本渲染
                    PayloadsJsonParser.MdEditorContentPayloads payloads = PayloadsJsonParser.parseJson(jsonString);

                    // 3. 生成 PDF (耗时操作)
                    return markdownService.convertHtmlToPdfBytes(payloads, baseUri, onMessage, onError);
                },
                this::updatePreviewUISvg, // 成功回调：调用 SVG Diff 更新逻辑
                e -> onError.accept("PDF preview failed: " + e.getMessage())
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

    private void updatePreviewUISvg(byte[] pdfBytes) {
        if (pdfBytes == null) return;

        // 后台计算 Diff
        new Thread(() -> {
            try {
                // 1. 计算 Diff
                var updates = pdfSvgService.diffPdfToSvg(pdfBytes);

                if (updates.isEmpty()) return; // 无变化，不打扰 UI

                // 2. 序列化
                String jsonString = new com.google.gson.Gson().toJson(updates);

                // 3. 推送前端
                Platform.runLater(() -> {
                    if (previewWebEngine == null) return;

                    String svgUrl = webServer.getBaseUrl() + "svg_preview.html";
                    String currentLoc = previewWebEngine.getLocation();

                    Runnable doUpdate = () -> {
                        try {
                            JSObject window = (JSObject) previewWebEngine.executeScript("window");
                            // 使用 call 避免转义问题
                            window.call("updateSvgPages", jsonString);
                        } catch (Exception e) {
                            log.error("JS Update failed", e);
                        }
                    };

                    // 确保页面加载
                    if (currentLoc == null || !currentLoc.startsWith(svgUrl)) {
                        previewWebEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                            if (state == Worker.State.SUCCEEDED) {
                                // 注入 Bridge 方便调试 (可选)
                                JSObject win = (JSObject) previewWebEngine.executeScript("window");
                                win.setMember("debugBridge", new DebugBridge());
                                doUpdate.run();
                            }
                        });
                        previewWebEngine.load(svgUrl);
                    } else {
                        doUpdate.run();
                    }
                });
            } catch (Exception e) {
                log.error("SVG Diff failed", e);
            }
        }).start();
    }

    /**
     * 更新预览 (图片流方案)
     * 流程：PDF -> 高清图片 -> 存入Server -> 通知前端 -> 前端静默预加载 -> 切换
     */
    private void updatePreviewUIImg(byte[] pdfBytes) {
        if (pdfBytes == null) return;

        new Thread(() -> {
            try {
                // 1. 计算 Diff 并渲染为图片
                // 返回的 updates 只包含元数据 (页码, version, 宽高)，不包含二进制图片数据
                var updates = pdfImageService.diffPdfToImages(pdfBytes);

                if (updates.isEmpty()) return;

                // 2. 将图片数据放入 WebServer 的内存缓存
                for (var update : updates) {
                    String imageKey = update.pageIndex() + ".png";

                    // 【核心修正】从 Service 获取二进制数据
                    byte[] imgData = pdfImageService.getImageData(update.pageIndex());

                    if (imgData != null) {
                        LocalWebServer.putImage(imageKey, imgData);
                    }
                }

                // 3. 序列化元数据为 JSON
                String jsonString = new com.google.gson.Gson().toJson(updates);

                // 4. UI 线程通知前端
                Platform.runLater(() -> {
                    try {
                        if (previewWebEngine == null) return;

                        String previewUrl = webServer.getBaseUrl() + "image_preview.html";
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
                            log.info("Loading Image Preview Engine...");
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