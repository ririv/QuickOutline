package com.ririv.quickoutline.api;

import com.ririv.quickoutline.api.service.RpcProcessor;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;

public class WebSocketRpcHandler implements Handler<ServerWebSocket> {
    private final RpcProcessor processor;

    public WebSocketRpcHandler(RpcProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void handle(ServerWebSocket ws) {
        // Accept /ws/tauri or just / if flexible, but user wants specific sets.
        // Let's check path in the handler or assume the caller filters.
        // Checking path here is safer.
        if (!ws.path().equals("/ws/tauri")) {
             // If this handler is used exclusively for this path, reject others?
             // Or maybe we chain them. For now, simple check.
             return; 
        }
        System.out.println("Tauri connected");
        ws.textMessageHandler(text -> {
            String response = processor.process(text);
            ws.writeFinalTextFrame(response);
        });
    }
}
