package com.ririv.quickoutline.api;

import com.ririv.quickoutline.api.service.RpcProcessor;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketRpcHandler implements Handler<ServerWebSocket> {
    private static final Logger log = LoggerFactory.getLogger(WebSocketRpcHandler.class);
    private final RpcProcessor processor;
    private final WebSocketSessionManager sessionManager;

    public WebSocketRpcHandler(RpcProcessor processor, WebSocketSessionManager sessionManager) {
        this.processor = processor;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(ServerWebSocket ws) {
        if (!ws.path().equals("/ws/tauri")) {
             return; 
        }
        log.info("Tauri connected via WebSocket");
        sessionManager.setSession(ws);
        
        ws.textMessageHandler(text -> {
            log.info("Received WebSocket message: {}", text);
            
            // Offload to worker thread to avoid blocking Event Loop
            Vertx.currentContext().owner().executeBlocking(() -> {
                try {
                    return processor.process(text);
                } catch (Exception e) {
                    log.error("RPC Processing Error", e);
                    throw new RuntimeException(e);
                }
            }).onComplete(res -> {
                if (res.succeeded()) {
                    String response = (String) res.result();
                    log.info("Sending WebSocket response: {}", response);
                    ws.writeFinalTextFrame(response);
                } else {
                    log.error("RPC Execution Failed", res.cause());
                }
            });
        });
        
        ws.closeHandler(v -> {
            log.info("Tauri WebSocket disconnected");
            sessionManager.clearSession();
        });
        ws.exceptionHandler(e -> log.error("WebSocket error", e));
    }
}