package com.ririv.quickoutline.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLConnection;

public class LocalWebServer {
    private static final Logger log = LoggerFactory.getLogger(LocalWebServer.class);
    private HttpServer server;
    private int port;

    /**
     * 启动服务器
     * @param baseResourcePath Classpath 下的 Web 根目录，例如 "/web"
     */
    public void start(String baseResourcePath) {
        try {
            // 绑定到本地回环地址 (localhost)，端口设为 0 表示自动分配空闲端口
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

            // 创建上下文处理所有请求
            server.createContext("/", new ClasspathHandler(baseResourcePath));

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setName("local-web-server-worker");
                t.setDaemon(true); // <--- 关键：设为守护线程，不设置可能会阻止 JVM 退出
                return t;
            }));
            server.start();

            this.port = server.getAddress().getPort();
            log.info("Local Web Server started at http://127.0.0.1:{}/ serving resources from {}", port, baseResourcePath);

        } catch (IOException e) {
            log.error("Failed to start local web server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("Local Web Server stopped.");
        }
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port + "/";
    }

    /**
     * 核心处理器：拦截 HTTP 请求 -> 读取 Classpath 资源 -> 返回给浏览器
     */
    private static class ClasspathHandler implements HttpHandler {
        private final String basePath;

        public ClasspathHandler(String basePath) {
            // 确保 basePath 以 / 开头，不以 / 结尾
            if (!basePath.startsWith("/")) basePath = "/" + basePath;
            if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
            this.basePath = basePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();

            // 处理默认页
            if (requestPath.equals("/")) {
                requestPath = "/editor.html";
            }

            // 拼接资源路径：/web + /editor.html
            String resourcePath = basePath + requestPath;

            // 1. 获取资源流
            InputStream is = getClass().getResourceAsStream(resourcePath);

            if (is == null) {
                // 404 Not Found
                String response = "404 Not Found: " + resourcePath;
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                log.warn("404 Not Found: {}", resourcePath);
                return;
            }

            // 2. 猜测 MIME 类型 (非常重要！否则 CSS/JS 可能无法加载)
            String mimeType = guessMimeType(resourcePath);

            // 3. 发送响应头
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            // 允许跨域 (可选，防止某些 JS 库报错)
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            exchange.sendResponseHeaders(200, 0); // 0 表示 chunked 传输或长度未知

            // 4. 发送文件内容
            try (is; OutputStream os = exchange.getResponseBody()) {
                is.transferTo(os);
            } catch (IOException e) {
                // 客户端可能中断连接，忽略
            }
        }

        private String guessMimeType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".woff")) return "font/woff";
            if (path.endsWith(".woff2")) return "font/woff2";
            if (path.endsWith(".ttf")) return "font/ttf";
            // 尝试使用系统猜测
            String type = URLConnection.guessContentTypeFromName(path);
            return type != null ? type : "application/octet-stream";
        }
    }
}