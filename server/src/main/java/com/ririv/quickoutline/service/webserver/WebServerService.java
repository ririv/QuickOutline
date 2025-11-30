package com.ririv.quickoutline.service.webserver;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton // 保证全应用只有一个实例
public class WebServerService {
    private static final Logger log = LoggerFactory.getLogger(WebServerService.class);

    private final LocalWebServer server;
    private boolean isStarted = false;

    public WebServerService() {
        this.server = new LocalWebServer();
    }

    public synchronized void start() {
        if (!isStarted) {
            // 映射 classpath 下的 /web 目录
            server.start("/web");
            isStarted = true;
            log.info("Global WebServer started.");
        }
    }

    public synchronized void stop() {
        if (isStarted) {
            server.stop();
            isStarted = false;
        }
    }

    public String getBaseUrl() {
        // 如果还没启动就调用，先启动
        if (!isStarted) start();
        return server.getBaseUrl();
    }
}
