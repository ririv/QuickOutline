package com.ririv.quickoutline.api;

import io.vertx.core.http.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketSessionManager {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);
    private ServerWebSocket currentSession;

    public void setSession(ServerWebSocket ws) {
        this.currentSession = ws;
    }

    public void clearSession() {
        this.currentSession = null;
    }

    public void sendEvent(String type, Object payload) {
        if (currentSession != null && !currentSession.isClosed()) {
            String json = String.format("{\"type\": \"%s\", \"payload\": %s}", type, payload); // Simple JSON for now, careful with payload escaping
            // Better use Gson if payload is complex object
            log.info("Pushing event: {}", json);
            currentSession.writeFinalTextFrame(json);
        } else {
            log.warn("No active WebSocket session to push event: {}", type);
        }
    }
    
    public void sendJson(String json) {
        if (currentSession != null && !currentSession.isClosed()) {
            currentSession.writeFinalTextFrame(json);
        }
    }
}
