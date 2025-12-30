package com.ririv.quickoutline.server;

import com.ririv.quickoutline.api.WebSocketSessionManager;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.api.service.RpcProcessor;
import com.ririv.quickoutline.api.WebSocketRpcHandler;
import com.ririv.quickoutline.api.service.impl.ApiServiceImpl;
import com.ririv.quickoutline.api.state.ApiBookmarkState;
import com.ririv.quickoutline.api.state.CurrentFileState;
import com.ririv.quickoutline.service.*;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

import java.io.IOException;

public class SidecarApp {
    public static void main(String[] args) throws IOException {
        // Parse arguments
        int port = 0;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i + 1]);
                }
            }
        }

        Vertx vertx = Vertx.vertx();

        // 1. 初始化服务
        PdfCheckService pdfCheckService = new PdfCheckService();
        PdfOutlineService pdfOutlineService = new PdfOutlineService();
        SyncWithExternalEditorService syncWithExternalEditorService = new SyncWithExternalEditorService();

        // 2. Initialize State and Managers
        ApiBookmarkState apiBookmarkState = new ApiBookmarkState();
        CurrentFileState currentFileState = new CurrentFileState();
        WebSocketSessionManager sessionManager = new WebSocketSessionManager();

        // 3. 初始化 API 实现
        ApiService apiService = new ApiServiceImpl(
                pdfCheckService,
                pdfOutlineService,
                apiBookmarkState,
                currentFileState,
                syncWithExternalEditorService,
                sessionManager
        );

        // 4. 初始化 RPC 处理器
        RpcProcessor rpcProcessor = new RpcProcessor(apiService);
        
        // 5. 初始化 WebSocket 处理器
        WebSocketRpcHandler tauriHandler = new WebSocketRpcHandler(rpcProcessor, sessionManager);

        // 6. 创建 HTTP 服务器
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
            String path = req.path();

            // Default Handler
            if (req.headers().get("Upgrade") == null || !req.headers().get("Upgrade").equalsIgnoreCase("websocket")) {
                req.response()
                   .putHeader("content-type", "text/plain; charset=utf-8")
                   .end("QuickOutline Sidecar is running.");
            }
        });

        // 8. 监听端口
        server.listen(port)
            .onSuccess(s -> {
                System.out.println("{\"port\": " + s.actualPort() + "}");
            })
            .onFailure(err -> {
                System.err.println("Failed to bind: " + err.getMessage());
                System.exit(1);
            });
    }

    private static void handleImageRequest(io.vertx.core.http.HttpServerRequest req, String path, String prefix, boolean cacheable, java.util.function.Function<Integer, java.util.concurrent.CompletableFuture<byte[]>> provider) {
        try {
            // Parse /prefix/0.png -> 0
            String[] parts = path.split("/");
            String pageNumStr = parts[parts.length - 1].split("\\.")[0];
            int pageIndex = Integer.parseInt(pageNumStr);

            provider.apply(pageIndex)
                .thenAccept(imageData -> {
                    if (imageData != null) {
                        var resp = req.response()
                           .putHeader("Access-Control-Allow-Origin", "*")
                           .putHeader("Content-Type", "image/png");
                        
                        if (cacheable) {
                            resp.putHeader("Cache-Control", "public, max-age=31536000");
                        } else {
                            resp.putHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        }
                           
                        resp.end(io.vertx.core.buffer.Buffer.buffer(imageData));
                    } else {
                        req.response().setStatusCode(404).end("Image not found");
                    }
                })
                .exceptionally(e -> {
                    req.response().setStatusCode(500).end("Error serving image: " + e.getMessage());
                    return null;
                });
        } catch (Exception e) {
            req.response().setStatusCode(500).end("Error parsing image request: " + e.getMessage());
        }
    }
}
