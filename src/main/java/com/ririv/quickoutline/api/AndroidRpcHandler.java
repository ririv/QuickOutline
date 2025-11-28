package com.ririv.quickoutline.api;

import com.ririv.quickoutline.api.service.RpcProcessor;

/**
 * Android WebView 专用处理器。
 * 在 Android 项目中，实例化此类并将其方法暴露给 WebView。
 */
public class AndroidRpcHandler {
    private final RpcProcessor processor;

    public AndroidRpcHandler(RpcProcessor processor) {
        this.processor = processor;
    }

    /**
     * 同步处理来自 Android WebView 的调用。
     * @param jsonRequest JSON RPC 请求字符串
     * @return JSON RPC 响应字符串
     */
    public String handle(String jsonRequest) {
        return processor.process(jsonRequest);
    }
}