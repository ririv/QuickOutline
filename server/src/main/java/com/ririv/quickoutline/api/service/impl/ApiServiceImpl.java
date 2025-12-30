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

import com.ririv.quickoutline.api.WebSocketSessionManager;
import jakarta.inject.Inject;

public class ApiServiceImpl implements ApiService {
    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);


    private final PdfCheckService pdfCheckService;
    private final ApiBookmarkState apiBookmarkState;
    private final CurrentFileState currentFileState;

    @Inject
    public ApiServiceImpl(PdfCheckService pdfCheckService,
                          ApiBookmarkState apiBookmarkState,
                          CurrentFileState currentFileState
                          ) {
        this.pdfCheckService = pdfCheckService;
        this.apiBookmarkState = apiBookmarkState;
        this.currentFileState = currentFileState;
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

}
