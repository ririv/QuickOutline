package com.ririv.quickoutline.server.websocket;

import com.ririv.quickoutline.api.service.RpcProcessor;
import com.ririv.quickoutline.api.service.impl.ApiServiceImpl;
import com.ririv.quickoutline.api.WebSocketRpcHandler;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator;
import com.ririv.quickoutline.service.FontManager; // Import FontManager
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import io.vertx.core.Vertx;

public class SidecarApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // 1. 初始化底层服务
        PdfOutlineService pdfOutlineService = new PdfOutlineService();
        
        FontManager fontManager = new FontManager(); // Instantiate FontManager
        TocPageGenerator tocPageGenerator = new iTextTocPageGenerator(fontManager); // Pass FontManager to constructor
        PdfTocPageGeneratorService pdfTocPageGeneratorService = new PdfTocPageGeneratorService(tocPageGenerator);
        
        PdfPageLabelService pdfPageLabelService = new PdfPageLabelService();

        // 2. 初始化 API 实现
        ApiServiceImpl apiService = new ApiServiceImpl(
                pdfOutlineService,
                pdfTocPageGeneratorService,
                pdfPageLabelService
        );

        // 3. 初始化 RPC 处理器
        RpcProcessor rpcProcessor = new RpcProcessor(apiService);
        
        // 4. 初始化 Tauri WebSocket 处理器
        WebSocketRpcHandler tauriHandler = new WebSocketRpcHandler(rpcProcessor);

        // AndroidRpcHandler 不需要在这里初始化，它是在 Android App 内部被实例化的

        vertx.createHttpServer()
                .requestHandler(req -> {
                    req.response()
                            .putHeader("content-type", "text/plain; charset=utf-8")
                            .end("QuickOutline Sidecar (Tauri) is running.");
                })
                .webSocketHandler(ws -> {
                    // 仅处理 Tauri 的连接
                    if (ws.path().equals("/ws/tauri")) {
                        tauriHandler.handle(ws);
                    } else {
                        // Replaced ws.reject() with ws.close()
                        ws.close((short) 1003, "Invalid WebSocket path"); // 1003 is "Unsupported Data" close code
                    }
                })
                .listen(0)
                .onSuccess(server -> {
                    // 打印端口给 Tauri 里的 Rust 读取
                    System.out.println("{\"port\": " + server.actualPort() + "}");
                })
                .onFailure(err -> {
                    System.err.println("Failed to bind: " + err.getMessage());
                    System.exit(1);
                });
    }
}