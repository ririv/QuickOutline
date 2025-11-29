package com.ririv.quickoutline.server;

import com.ririv.quickoutline.api.service.RpcProcessor;
import com.ririv.quickoutline.api.service.impl.ApiServiceImpl;
import com.ririv.quickoutline.api.WebSocketRpcHandler;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator;
import com.ririv.quickoutline.service.FontManager;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.service.pdfpreview.PdfImageService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

public class SidecarApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // 1. 初始化服务，共享 PdfImageService
        PdfImageService pdfImageService = new PdfImageService();
        PdfOutlineService pdfOutlineService = new PdfOutlineService();
        FontManager fontManager = new FontManager();
        TocPageGenerator tocPageGenerator = new iTextTocPageGenerator(fontManager);
        PdfTocPageGeneratorService pdfTocPageGeneratorService = new PdfTocPageGeneratorService(tocPageGenerator);
        PdfPageLabelService pdfPageLabelService = new PdfPageLabelService();

        // 2. 初始化 API 实现
        ApiServiceImpl apiService = new ApiServiceImpl(
                pdfOutlineService,
                pdfTocPageGeneratorService,
                pdfPageLabelService,
                pdfImageService // 注入共享的实例
        );

        // 3. 初始化 RPC 处理器
        RpcProcessor rpcProcessor = new RpcProcessor(apiService);
        
        // 4. 初始化 WebSocket 处理器
        WebSocketRpcHandler tauriHandler = new WebSocketRpcHandler(rpcProcessor);

        // 5. 创建 HTTP 服务器
        HttpServer server = vertx.createHttpServer();

        // 6. 配置 WebSocket 处理器
        server.webSocketHandler(ws -> {
            if (ws.path().equals("/ws/tauri")) {
                tauriHandler.handle(ws);
            } else {
                ws.close((short) 1003, "Invalid WebSocket path");
            }
        });

        // 7. 配置 HTTP 请求处理器
        server.requestHandler(req -> {
            // 图片服务
            if (req.path().startsWith("/page_images/")) {
                try {
                    // 解析 /page_images/0.png -> 0
                    String[] parts = req.path().split("/");
                    String pageNumStr = parts[parts.length - 1].split("\\.")[0];
                    int pageIndex = Integer.parseInt(pageNumStr);

                    byte[] imageData = apiService.getPreviewImageData(pageIndex);

                    if (imageData != null) {
                        req.response()
                           .putHeader("Content-Type", "image/png")
                           .putHeader("Cache-Control", "public, max-age=31536000") // 强缓存
                           .end(io.vertx.core.buffer.Buffer.buffer(imageData));
                    } else {
                        req.response().setStatusCode(404).end("Image not found");
                    }
                } catch (Exception e) {
                    req.response().setStatusCode(500).end("Error serving image: " + e.getMessage());
                }
            } 
            // 如果不是 WebSocket 升级请求，也不是图片请求，则返回欢迎页
            else if (req.headers().get("Upgrade") == null || !req.headers().get("Upgrade").equalsIgnoreCase("websocket")) {
                req.response()
                   .putHeader("content-type", "text/plain; charset=utf-8")
                   .end("QuickOutline Sidecar is running.");
            }
            // (如果是 WebSocket 升级请求，则由 webSocketHandler 处理)
        });

        // 8. 监听端口
        server.listen(0)
            .onSuccess(s -> {
                System.out.println("{\"port\": " + s.actualPort() + "}");
            })
            .onFailure(err -> {
                System.err.println("Failed to bind: " + err.getMessage());
                System.exit(1);
            });
    }
}
