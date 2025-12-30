package com.ririv.quickoutline.api.service.impl;

import com.google.gson.Gson;
import com.ririv.quickoutline.api.state.ApiBookmarkState;
import com.ririv.quickoutline.api.state.CurrentFileState;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.*;
import com.ririv.quickoutline.textProcess.methods.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.api.WebSocketSessionManager;
import jakarta.inject.Inject;

public class ApiServiceImpl implements ApiService {
    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);

    private final PdfOutlineService pdfOutlineService;

    private final PdfCheckService pdfCheckService;
    private final ApiBookmarkState apiBookmarkState;
    private final CurrentFileState currentFileState;
    private final SyncWithExternalEditorService syncService;
    private final WebSocketSessionManager sessionManager;

    @Inject
    public ApiServiceImpl(PdfCheckService pdfCheckService,
                          PdfOutlineService pdfOutlineService,
                          ApiBookmarkState apiBookmarkState,
                          CurrentFileState currentFileState,
                          SyncWithExternalEditorService syncService,
                          WebSocketSessionManager sessionManager) {
        this.pdfCheckService = pdfCheckService;
        this.pdfOutlineService = pdfOutlineService;
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
