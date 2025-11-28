package com.ririv.quickoutline.server.websocket;
import io.vertx.core.Vertx;

public class SidecarApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.createHttpServer()
                // 1. 处理普通 HTTP 请求 (浏览器访问时显示)
                .requestHandler(req -> {
                    req.response()
                            .putHeader("content-type", "text/plain; charset=utf-8")
                            .end("Java Sidecar is running! \n请使用 WebSocket 连接此端口进行通信。");
                })
                // 2. 处理 WebSocket 连接 (Tauri 通信时使用)
                .webSocketHandler(ws -> {
                    System.out.println("Tauri 前端已连接");

                    // 接收前端的指令
                    ws.handler(data -> {
                        String command = data.toString();
                        System.out.println("收到指令: " + command);

                        // 模拟处理：实时发回预览数据
                        ws.writeFinalTextFrame("{\"preview\": \"Data processed: " + command + "\"}");
                    });
                })
                // Vert.x 5 变更：listen() 返回 Future，不再接受 Handler 回调
                .listen(0)
                .onSuccess(server -> {
                    // 关键：将端口打印到 stdout 供 Tauri 读取
                    System.out.println("{\"port\": " + server.actualPort() + "}");
                })
                .onFailure(err -> {
                    System.err.println("Failed to bind: " + err.getMessage());
                    System.exit(1);
                });
    }
}