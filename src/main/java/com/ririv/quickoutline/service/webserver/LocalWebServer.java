package com.ririv.quickoutline.service.webserver;

import com.ririv.quickoutline.service.pdfpreview.PdfImageService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class LocalWebServer {
    private static final Logger log = LoggerFactory.getLogger(LocalWebServer.class);

    private HttpServer server;
    private int port;

    // 单例实例
    private static LocalWebServer instance;

    public static synchronized LocalWebServer getInstance() {
        if (instance == null) {
            instance = new LocalWebServer();
        }
        return instance;
    }

    // 默认文档 ID，用于向后兼容
    public static final String DEFAULT_DOC_ID = "default";

    // 存储每个文档的 PDF 数据: DocID -> PDF Bytes
    private final Map<String, byte[]> documentPdfBytes = new java.util.concurrent.ConcurrentHashMap<>();

    // 存储每个文档的图片数据: DocID -> (PageKey -> Bytes)
    private final Map<String, Map<String, byte[]>> documentImages = new java.util.concurrent.ConcurrentHashMap<>();

    public void putImage(String docId, String path, byte[] data) {
        documentImages.computeIfAbsent(docId, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(path, data);
    }

    // 兼容旧 API
    public static void putImage(String path, byte[] data) {
        getInstance().putImage(DEFAULT_DOC_ID, path, data);
    }

    /**
     * 更新内存中的 PDF 数据
     * @param docId 文档 ID
     * @param bytes PDF 文件的二进制数据
     */
    public void updatePdf(String docId, byte[] bytes) {
        documentPdfBytes.put(docId, bytes);
    }

    // 兼容旧 API
    public void updatePdf(byte[] bytes) {
        updatePdf(DEFAULT_DOC_ID, bytes);
    }

    /**
     * 启动服务器
     * @param baseResourcePath Classpath 下的 Web 根目录，例如 "/web"
     */
    public synchronized void start(String baseResourcePath) {
        if (server != null) {
            log.info("Local Web Server is already running at {}", getBaseUrl());
            return;
        }

        try {
            // 1. 绑定到本地回环地址 (localhost)，端口设为 0 表示自动分配空闲端口
            // 防止防火墙拦截公网访问，增强安全性
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

            // 2. 配置线程池：使用守护线程 (Daemon Thread)
            // 关键点：当主程序(JavaFX)退出时，JVM 会自动杀死守护线程，防止程序卡死在后台
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "local-web-server-worker");
                t.setDaemon(true);
                return t;
            }));

            // 3. 注册静态资源处理器 (处理 html, js，css 等)
            server.createContext("/", new ClasspathHandler(baseResourcePath));

            // 4. 注册动态 PDF 处理器 (处理预览数据)
            // 支持 /dynamic_preview.pdf (默认) 和 /dynamic_preview/{docId}.pdf
            server.createContext("/dynamic_preview", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String docId = DEFAULT_DOC_ID;
                
                if (path.endsWith(".pdf") && path.length() > "/dynamic_preview/".length()) {
                     // 解析 /dynamic_preview/abc-123.pdf -> abc-123
                     String fileName = path.substring("/dynamic_preview/".length());
                     docId = fileName.substring(0, fileName.length() - 4);
                }

                byte[] data = documentPdfBytes.get(docId);
                if (data == null && DEFAULT_DOC_ID.equals(docId)) {
                     // 尝试回退到旧逻辑的默认值（虽然现在统一用 map 了，但可以给个空数组防空指针）
                     data = new byte[0]; 
                }

                if (data == null || data.length == 0) {
                    String msg = "PDF generating...";
                    exchange.sendResponseHeaders(404, msg.length());
                    try (OutputStream os = exchange.getResponseBody()) { os.write(msg.getBytes()); }
                    return;
                }

                // 设置响应头
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                // 禁止缓存，确保每次刷新都能看到最新的 PDF
                exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
                exchange.getResponseHeaders().set("Pragma", "no-cache");
                exchange.getResponseHeaders().set("Expires", "0");

                // 发送数据
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            });

            server.start();
            this.port = server.getAddress().getPort();
            log.info("Local Web Server started at http://127.0.0.1:{}/ serving resources from {}", port, baseResourcePath);

        } catch (IOException e) {
            log.error("Failed to start local web server", e);
            throw new RuntimeException("Could not start internal web server", e);
        }
        // 注册图片 Handler
        // 支持 /page_images/0.png (默认) 和 /page_images/{docId}/0.png
        server.createContext("/page_images/", exchange -> {
            // URL 类似: /page_images/0.png?v=123456  或者 /page_images/doc-123/0.png?v=...
            String path = exchange.getRequestURI().getPath();
            String subPath = path.substring("/page_images/".length()); 
            
            String docId = DEFAULT_DOC_ID;
            String key = subPath;

            // 简单判断：如果包含斜杠，说明是 {docId}/{key} 格式
            if (subPath.contains("/")) {
                int slashIndex = subPath.indexOf("/");
                docId = subPath.substring(0, slashIndex);
                key = subPath.substring(slashIndex + 1);
            }

            byte[] data = null;
            Map<String, byte[]> pages = documentImages.get(docId);
            if (pages != null) {
                data = pages.get(key);
            }

            if (data == null) {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "image/png");
            // 强缓存控制：因为 URL 带了 version，我们可以让浏览器永久缓存这个 URL
            // 这样滚动回去时不需要重新下载
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000");

            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.stop(0); // 立即停止，不等待
            log.info("Local Web Server stopped.");
        }
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port + "/";
    }

    /**
     * 静态资源处理器：拦截 HTTP 请求 -> 读取 Classpath 资源 -> 返回给浏览器
     */
    private static class ClasspathHandler implements HttpHandler {
        private final String basePath;

        public ClasspathHandler(String basePath) {
            // 规范化路径，确保以 / 开头，不以 / 结尾
            if (!basePath.startsWith("/")) basePath = "/" + basePath;
            if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
            this.basePath = basePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();

            // 默认首页
            if (requestPath.equals("/")) {
                requestPath = "/vditor.html";
            }

            // 拼接资源路径：/web + /path/to/file.js
            String resourcePath = basePath + requestPath;

            // 从 Classpath (Jar包内部) 读取流
            InputStream is = getClass().getResourceAsStream(resourcePath);

            if (is == null) {
                // 404 Not Found
                String response = "404 Not Found: " + resourcePath;
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                // 降低日志级别，避免 favicon.ico 等缺失刷屏
                if (!resourcePath.endsWith("favicon.ico")) {
                    log.warn("404 Not Found: {}", resourcePath);
                }
                return;
            }

            // 猜测 MIME 类型，否则 CSS/JS 可能无法加载
            String mimeType = guessMimeType(resourcePath);
            exchange.getResponseHeaders().set("Content-Type", mimeType);

            // 允许跨域 (可选，防止某些 JS 库报错)
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            // 发送响应
            exchange.sendResponseHeaders(200, 0); // 0 代表 chunked transfer encoding

            try (is; OutputStream os = exchange.getResponseBody()) {
                is.transferTo(os);
            } catch (IOException e) {
                // 客户端（浏览器）可能在下载中途断开连接，忽略此类错误
            }
        }

        private String guessMimeType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".mjs")) return "application/javascript";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".gif")) return "image/gif";
            if (path.endsWith(".woff")) return "font/woff";
            if (path.endsWith(".woff2")) return "font/woff2";
            if (path.endsWith(".ttf")) return "font/ttf";
            if (path.endsWith(".eot")) return "application/vnd.ms-fontobject";
            if (path.endsWith(".pdf")) return "application/pdf";

            // 尝试系统推断
            String type = URLConnection.guessContentTypeFromName(path);
            return type != null ? type : "application/octet-stream";
        }
    }
}