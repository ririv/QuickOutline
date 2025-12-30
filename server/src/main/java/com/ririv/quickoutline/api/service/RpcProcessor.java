package com.ririv.quickoutline.api.service;

import com.google.gson.Gson;
import com.ririv.quickoutline.api.model.RpcRequest;
import com.ririv.quickoutline.api.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcProcessor {
    private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);
    private final ApiService apiService;
    private final Gson gson = new Gson();

    public RpcProcessor(ApiService apiService) {
        this.apiService = apiService;
    }

    public String process(String jsonRequest) {
        RpcRequest request = null;
        try {
            log.debug("Processing RPC request: {}", jsonRequest);
            request = gson.fromJson(jsonRequest, RpcRequest.class);
            if (request == null) throw new IllegalArgumentException("Empty request");
            
            log.info("Executing method: {}", request.method);
            Object result = null;

            switch (request.method) {
                case "placeholder":
//                    apiService.placeholder();
                    result = "OK";
                    break;

                default:
                    throw new IllegalArgumentException("Unknown method: " + request.method);
            }
            return gson.toJson(RpcResponse.success(request.id, result));
        } catch (Exception e) {
            log.error("RPC Error processing request: {}", jsonRequest, e);
            String id = (request != null) ? request.id : null;
            return gson.toJson(RpcResponse.error(id, e.getMessage()));
        }
    }
}