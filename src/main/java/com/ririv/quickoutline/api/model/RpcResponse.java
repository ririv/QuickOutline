package com.ririv.quickoutline.api.model;

public class RpcResponse {
    public String id;
    public Object result;
    public String error;

    public RpcResponse(String id, Object result, String error) {
        this.id = id;
        this.result = result;
        this.error = error;
    }
    
    public static RpcResponse success(String id, Object result) {
        return new RpcResponse(id, result, null);
    }
    
    public static RpcResponse error(String id, String error) {
        return new RpcResponse(id, null, error);
    }
}
