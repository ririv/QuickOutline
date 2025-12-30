package com.ririv.quickoutline.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.api.model.RpcRequest;
import com.ririv.quickoutline.api.model.RpcResponse;
import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.utils.RpcUtils;
import com.ririv.quickoutline.model.ViewScaleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class RpcProcessor {
    private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);
    private final ApiService apiService;
    private final Gson gson = new Gson();

    public RpcProcessor(ApiService apiService) {
        this.apiService = apiService;
    }

    private ViewScaleType parseViewMode(Object obj) {
        if (obj instanceof String) {
            try {
                return ViewScaleType.valueOf((String) obj);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid ViewScaleType: {}", obj);
            }
        }
        return ViewScaleType.NONE;
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
                case "openFile":
                    apiService.openFile((String) request.params.get(0));
                    result = "OK";
                    break;
                case "getOutline":
                    result = apiService.getOutline(RpcUtils.getInt(request.params.get(0)));
                    break;
                case "getOutlineAsBookmark":
                    result = apiService.getOutlineAsBookmark(RpcUtils.getInt(request.params.get(0)));
                    break;
                case "saveOutline":
                    // params: [bookmarkDto (nullable), destPath, offset, viewMode]
                    Bookmark root = null;
                    if (request.params.get(0) != null) {
                        JsonElement bookmarkJson = gson.toJsonTree(request.params.get(0));
                        BookmarkDto dto = gson.fromJson(bookmarkJson, BookmarkDto.class);
                        if (dto != null) {
                            root = dto.toDomain();
                        }
                    }
                    String dest = request.params.size() > 1 ? (String) request.params.get(1) : null;
                    int offset = request.params.size() > 2 ? RpcUtils.getInt(request.params.get(2)) : 0;
                    ViewScaleType viewMode = request.params.size() > 3 ? parseViewMode(request.params.get(3)) : ViewScaleType.NONE;
                    apiService.saveOutline(root, dest, offset, viewMode);
                    result = "OK";
                    break;
                case "getCurrentFilePath":
                    result = apiService.getCurrentFilePath();
                    break;
                
                // --- TOC ---
                case "generateTocPage":
                    // params: [TocConfig, destPath]
                    TocConfig tocConfig = gson.fromJson(gson.toJsonTree(request.params.get(0)), TocConfig.class);
                    String tocDest = request.params.size() > 1 ? (String) request.params.get(1) : null;
                    apiService.generateTocPage(tocConfig, tocDest);
                    result = "OK";
                    break;

                // --- Sync Utils ---
                case "parseTextToTree":
                    // params: [text]
                    result = apiService.parseTextToTree((String) request.params.get(0));
                    break;
                case "updateOffset":
                    // params: [offset]
                    apiService.updateOffset(RpcUtils.getInt(request.params.get(0)));
                    result = "OK";
                    break;
                case "serializeTreeToText":
                    // params: [rootBookmarkDto]
                    JsonElement dtoElement = gson.toJsonTree(request.params.get(0));
                    BookmarkDto dto = gson.fromJson(dtoElement, BookmarkDto.class);
                    Bookmark domainRoot = dto != null ? dto.toDomain() : null;
                    result = apiService.serializeTreeToText(domainRoot);
                    break;
                case "openExternalEditor":
                    // params: [text]
                    apiService.openExternalEditor((String) request.params.get(0));
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