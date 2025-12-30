package com.ririv.quickoutline.api.service.impl;

import com.ririv.quickoutline.api.state.CurrentFileState;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class ApiServiceImpl implements ApiService {
    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);


    private final PdfCheckService pdfCheckService;
    private final CurrentFileState currentFileState;

    @Inject
    public ApiServiceImpl(PdfCheckService pdfCheckService,
                          CurrentFileState currentFileState
                          ) {
        this.pdfCheckService = pdfCheckService;
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

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCurrentFilePath() {
        return currentFileState.getFilePath();
    }

}
