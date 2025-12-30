package com.ririv.quickoutline.api.service.impl;

import com.google.gson.Gson;
import com.ririv.quickoutline.api.state.ApiBookmarkState;
import com.ririv.quickoutline.api.state.CurrentFileState;
import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.model.PageLabel;
import com.ririv.quickoutline.model.ViewScaleType;
import com.ririv.quickoutline.service.*;
import com.ririv.quickoutline.textProcess.methods.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.api.WebSocketSessionManager;
import jakarta.inject.Inject;

public class ApiServiceImpl implements ApiService {
    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);

    private final PdfOutlineService pdfOutlineService;

    private final PdfCheckService pdfCheckService;
    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final ApiBookmarkState apiBookmarkState;
    private final CurrentFileState currentFileState;
    private final SyncWithExternalEditorService syncService;
    private final WebSocketSessionManager sessionManager;

    @Inject
    public ApiServiceImpl(PdfCheckService pdfCheckService,
                          PdfOutlineService pdfOutlineService,
                          PdfTocPageGeneratorService pdfTocPageGeneratorService,
                          ApiBookmarkState apiBookmarkState,
                          CurrentFileState currentFileState,
                          SyncWithExternalEditorService syncService,
                          WebSocketSessionManager sessionManager) {
        this.pdfCheckService = pdfCheckService;
        this.pdfOutlineService = pdfOutlineService;
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.apiBookmarkState = apiBookmarkState;
        this.currentFileState = currentFileState;
        this.syncService = syncService;
        this.sessionManager = sessionManager;
    }

    private void checkFileOpen() {
        if (!currentFileState.isExist()) throw new IllegalStateException("No file open");
    }

    @Override
    public void openFile(String filePath) {
        try {
            pdfCheckService.checkOpenFile(filePath);
            currentFileState.open(filePath);

            this.apiBookmarkState.clear(); // Clear state on new file
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCurrentFilePath() {
        return currentFileState.getFilePath();
    }

    @Override
    public String getOutline(int offset) {
        checkFileOpen();
        try {
            String content = pdfOutlineService.getContents(currentFileState.getFilePath(), offset);

            // Sync State: Parse back to object to hold in memory
            Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(content, Method.INDENT);
            apiBookmarkState.setRootBookmark(root);
            apiBookmarkState.setOffset(offset);

            return content;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BookmarkDto getOutlineAsBookmark(int offset) {
        checkFileOpen();
        try {
            Bookmark root = pdfOutlineService.getOutlineAsBookmark(currentFileState.getFilePath(), offset);

            // Sync State
            apiBookmarkState.setRootBookmark(root);
            apiBookmarkState.setOffset(offset);

            return BookmarkDto.fromDomain(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void updateOffset(int offset) {
        apiBookmarkState.setOffset(offset);
    }

    private String resolveDestFilePath(String destFilePath) {
        if (destFilePath != null && !destFilePath.trim().isEmpty()) {
            return destFilePath;
        }
        return calculateAutoDestPath(currentFileState.getFilePath());
    }

    private String calculateAutoDestPath(String srcPath) {
        File srcFile = new File(srcPath);
        String fileName = srcFile.getName();
        String parent = srcFile.getParent();

        int dotIndex = fileName.lastIndexOf('.');
        String name = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        String ext = (dotIndex == -1) ? "" : fileName.substring(dotIndex);

        // 1. Try base suffix _new
        String candidateName = name + "_new" + ext;
        File candidateFile = new File(parent, candidateName);

        if (!candidateFile.exists()) {
            return candidateFile.getAbsolutePath();
        }

        // 2. Collision detected, increment: _new_1, _new_2...
        int counter = 1;
        while (candidateFile.exists()) {
            candidateName = name + "_new_" + counter + ext;
            candidateFile = new File(parent, candidateName);
            counter++;
        }

        return candidateFile.getAbsolutePath();
    }

    @Override
    public void saveOutline(Bookmark rootBookmark, String destFilePath, int offset, ViewScaleType viewMode) {
        checkFileOpen();
        String actualDest = resolveDestFilePath(destFilePath);

        // Strategy: Use provided params if present (stateless call),
        // otherwise fallback to state (stateful call).
        // For standard flow, we prefer the state if it exists and matches context.

        Bookmark targetRoot = rootBookmark;
        int targetOffset = offset;

        if (targetRoot == null && apiBookmarkState.hasRootBookmark()) {
            targetRoot = apiBookmarkState.getRootBookmark();
            targetOffset = apiBookmarkState.getOffset();
            log.info("Saving outline using Server State (offset={})", targetOffset);
        }

        if (targetRoot == null) {
            throw new IllegalArgumentException("No bookmark data provided and no server state available.");
        }

        // Use the provided viewMode directly, defaulting to NONE if null (though RpcProcessor handles default)
        ViewScaleType scaleType = (viewMode != null) ? viewMode : ViewScaleType.NONE;

        try {
            pdfOutlineService.setOutline(targetRoot, currentFileState.getFilePath(), actualDest, targetOffset, scaleType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void generateTocPage(TocConfig config, String destFilePath) {
        checkFileOpen();
        String actualDest = resolveDestFilePath(destFilePath);
        Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(config.tocContent(), Method.INDENT);

        try {
            pdfTocPageGeneratorService.createTocPage(
                    currentFileState.getFilePath(),
                    actualDest,
                    config.title(),
                    config.insertPos(),
                    config.numberingStyle(),
                    root,
                    config.header(),
                    config.footer(),
                    config.links(),
                    msg -> log.info("TOC Gen msg: {}", msg),
                    err -> {
                        throw new RuntimeException(err);
                    }
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public BookmarkDto parseTextToTree(String text) {
        Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(text, Method.INDENT);
        return BookmarkDto.fromDomain(root);
    }

    @Override
    public String serializeTreeToText(Bookmark root) {
        if (root == null) return "";
        return root.toOutlineString();
    }

    @Override
    public void openExternalEditor(String textContent) {
        // Start the sync process
        syncService.exec(
            null, // Coordinate pos, null for now
            fileText -> {
                // Sync callback: External file changed
                log.info("External editor content changed.");
                
                // 1. Update backend state
                // We need to parse the text to tree to update the state completely? 
                // Or just trust the text?
                // Let's update the state properly so other parts of the app are in sync.
                Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(fileText, Method.INDENT);
                apiBookmarkState.setRootBookmark(root);
                
                // 2. Push to frontend
                // We push the text content so the frontend editor can update
                // Event type: "external-editor-update"
                // Payload: { "text": "..." }
                // Since our sessionManager.sendEvent takes object and JSON-stringifies it, 
                // we can wrap it in a map or DTO.
                Map<String, String> payload = new HashMap<>();
                payload.put("text", fileText);
                sessionManager.sendEvent("external-editor-update", new Gson().toJsonTree(payload));
            },
            () -> {
                // Before callback
                log.info("External editor starting...");
                syncService.writeTemp(textContent);
                sessionManager.sendEvent("external-editor-start", null);
            },
            () -> {
                // After callback (Closed)
                log.info("External editor closed.");
                sessionManager.sendEvent("external-editor-end", null);
            },
            () -> {
                // Error callback
                log.error("External editor error.");
                sessionManager.sendEvent("external-editor-error", "Failed to launch or sync with external editor.");
            }
        );
    }

}
