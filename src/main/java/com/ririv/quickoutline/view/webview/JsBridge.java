package com.ririv.quickoutline.view.webview;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class JsBridge {

    private static final Logger log = LoggerFactory.getLogger(JsBridge.class);

    private Consumer<String> onPreviewToc;
    private Consumer<String> onGenerateToc;
    private Consumer<String> onRenderPdf;

    public void setTocHandlers(Consumer<String> onPreview, Consumer<String> onGenerate) {
        this.onPreviewToc = onPreview;
        this.onGenerateToc = onGenerate;
    }

    public void setRenderPdfHandler(Consumer<String> handler) {
        this.onRenderPdf = handler;
    }

    // Called by JS
    public void previewToc(String json) {
        if (onPreviewToc != null) {
            // Ensure callback runs on JavaFX thread if not already
            // But Platform.runLater is safe even if already on FX thread
            Platform.runLater(() -> onPreviewToc.accept(json));
        }
    }

    // Called by JS
    public void generateToc(String json) {
        if (onGenerateToc != null) {
            Platform.runLater(() -> onGenerateToc.accept(json));
        }
    }

    // Called by JS (Markdown Tab)
    public void renderPdf(String json) {
        if (onRenderPdf != null) {
            Platform.runLater(() -> onRenderPdf.accept(json));
        }
    }

    // 这个 Future 将用来“挂起”等待 JS 的结果
    private CompletableFuture<String> pendingFuture;

    // JS 将调用这个方法来"完成" pendingFuture
    public void receiveSuccess(String html) {
        log.debug("[JsBridge] 接收到 HTML");
        // 确保在 JavaFX 线程上完成 Future，这更安全
        Platform.runLater(() -> {
            if (pendingFuture != null && !pendingFuture.isDone()) {
                pendingFuture.complete(html);
            }
        });
    }

    // JS 发生错误时调用
    public void receiveError(String error) {
        Platform.runLater(() -> {
            if (pendingFuture != null && !pendingFuture.isDone()) {
                pendingFuture.completeExceptionally(new RuntimeException(error));
            }
        });
    }

    /**
     * 这是您将从 Java 调用的“同步”方法！
     * @param webEngine
     * @return
     */
    public String getContentSync(WebEngine webEngine) throws Exception {
        // 1. 创建一个新的 Future 来等待
        this.pendingFuture = new CompletableFuture<>();

        // 2. 准备 JS 脚本，让它在完成后回调我们的 bridge
        String script = """
            window.getPayloads()
                .then(jsonContent => {
                    window.javaBridge.receiveSuccess(jsonContent);
                })
                .catch(error => {
                    window.javaBridge.receiveError(String(error));
                });
        """;

        // 3. 关键：我们必须在 JavaFX 线程上执行 executeScript
        // 但我们不能在 JavaFX 线程上 "get()" (阻塞)
        // 所以我们使用 Platform.runLater 来 *触发* JS
        Platform.runLater(() -> {
            webEngine.executeScript(script);
        });

        // 4. 等待！
        // 我们在这里“同步”阻塞了 *调用 getHtmlSync 的这个线程*
        // 等待 JS 调用 receiveHtml() 来 complete 这个 Future
        // 我们加一个超时（比如 10 秒）以防万一
        try {
            return pendingFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取 HTML 失败 (超时或 JS 异常)");
            throw e;
        }
    }

    // Clipboard support
    public void copyText(String text) {
        // Directly access clipboard on the current thread (which is FX thread for WebView bridge calls)
        // to ensure synchronous update and avoid race conditions.
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    }

    public String getClipboardText() {
        // WebEngine executes JS bridge calls on the JavaFX Application Thread,
        // so it is safe to access the Clipboard directly.
        if (javafx.scene.input.Clipboard.getSystemClipboard().hasString()) {
            return javafx.scene.input.Clipboard.getSystemClipboard().getString();
        }
        return "";
    }

    // 用来存放最新的 PDF Base64 字符串
    private final AtomicReference<String> currentPdfBase64 = new AtomicReference<>("");

    // Java 端调用：更新数据
    public void setPdfData(byte[] pdfBytes) {
        if (pdfBytes != null) {
            this.currentPdfBase64.set(Base64.getEncoder().encodeToString(pdfBytes));
        }
    }

    // JS 端调用：获取数据
    // 注意：方法必须是 public
    public String getPdfData() {
        return currentPdfBase64.get();
    }

    public static class DebugBridge {
        public void log(String msg) {
            log.info("[TOC-JS] {}", msg); }
        public void error(String msg) { log.error("[TOC-JS Error] {}", msg); }
    }
}