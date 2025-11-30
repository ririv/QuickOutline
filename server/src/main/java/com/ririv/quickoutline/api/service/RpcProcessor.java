package com.ririv.quickoutline.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.api.model.RpcRequest;
import com.ririv.quickoutline.api.model.RpcResponse;
import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PageLabelRule;
import com.ririv.quickoutline.utils.RpcUtils;
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
                    // params: [bookmarkDto (nullable), destPath, offset]
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
                    apiService.saveOutline(root, dest, offset);
                    result = "OK";
                    break;
                case "saveOutlineFromText":
                    // params: [text, destPath, offset]
                    String text = (String) request.params.get(0);
                    String txtDest = request.params.size() > 1 ? (String) request.params.get(1) : null;
                    int txtOffset = request.params.size() > 2 ? RpcUtils.getInt(request.params.get(2)) : 0;
                    apiService.saveOutlineFromText(text, txtDest, txtOffset);
                    result = "OK";
                    break;
                case "autoFormat":
                    result = apiService.autoFormat((String) request.params.get(0));
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
                case "generateTocPreview":
                    // params: [TocConfig]
                    TocConfig previewConfig = gson.fromJson(gson.toJsonTree(request.params.get(0)), TocConfig.class);
                    result = apiService.generateTocPreview(previewConfig);
                    break;

                // --- Page Labels ---
                case "getPageLabels":
                    // params: [srcPath (optional)]
                    String srcPath = request.params.isEmpty() || request.params.get(0) == null ? null : (String) request.params.get(0);
                    result = apiService.getPageLabels(srcPath);
                    break;
                case "setPageLabels":
                    // params: [List<PageLabelRule>, destPath]
                    Type ruleListType = new TypeToken<List<PageLabelRule>>(){}.getType();
                    List<PageLabelRule> rules = gson.fromJson(gson.toJsonTree(request.params.get(0)), ruleListType);
                    String labelDest = request.params.size() > 1 ? (String) request.params.get(1) : null;
                    apiService.setPageLabels(rules, labelDest);
                    result = "OK";
                    break;
                case "simulatePageLabels":
                    // params: [List<PageLabelRule>]
                    Type simRuleListType = new TypeToken<List<PageLabelRule>>(){}.getType();
                    List<PageLabelRule> simRules = gson.fromJson(gson.toJsonTree(request.params.get(0)), simRuleListType);
                    result = apiService.simulatePageLabels(simRules);
                    break;

                case "getPageCount":
                    result = apiService.getPageCount();
                    break;

                // --- Sync Utils ---
                case "parseTextToTree":
                    // params: [text]
                    result = apiService.parseTextToTree((String) request.params.get(0));
                    break;
                case "syncFromText":
                    // params: [text]
                    result = apiService.syncFromText((String) request.params.get(0));
                    break;
                case "syncFromTree":
                    // params: [BookmarkDto]
                    JsonElement treeDtoJson = gson.toJsonTree(request.params.get(0));
                    BookmarkDto treeDto = gson.fromJson(treeDtoJson, BookmarkDto.class);
                    result = apiService.syncFromTree(treeDto);
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