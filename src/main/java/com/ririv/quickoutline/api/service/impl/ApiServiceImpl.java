package com.ririv.quickoutline.api.service.impl;

import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.ViewScaleType; // Correctly imported
import com.ririv.quickoutline.service.PageLabelRule;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.textProcess.methods.Method;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class ApiServiceImpl implements ApiService {
    private final PdfOutlineService pdfOutlineService;
    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final PdfPageLabelService pdfPageLabelService;
    
    private String currentFilePath;

    public ApiServiceImpl(PdfOutlineService pdfOutlineService,
                          PdfTocPageGeneratorService pdfTocPageGeneratorService,
                          PdfPageLabelService pdfPageLabelService) {
        this.pdfOutlineService = pdfOutlineService;
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.pdfPageLabelService = pdfPageLabelService;
    }

    private void checkFileOpen() {
        if (currentFilePath == null) throw new IllegalStateException("No file open");
    }

    @Override
    public void openFile(String filePath) {
        try {
            pdfOutlineService.checkOpenFile(filePath);
            this.currentFilePath = filePath;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCurrentFilePath() {
        return currentFilePath;
    }

    @Override
    public String getOutline(int offset) {
        checkFileOpen();
        try {
            return pdfOutlineService.getContents(currentFilePath, offset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Bookmark getOutlineAsBookmark(int offset) {
        checkFileOpen();
        try {
            return pdfOutlineService.getOutlineAsBookmark(currentFilePath, offset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOutline(Bookmark rootBookmark, String destFilePath, int offset) {
        checkFileOpen();
        String actualDest = destFilePath != null ? destFilePath : currentFilePath;
        try {
            // Corrected ViewScaleType usage: replaced .XYZ with .NONE
            pdfOutlineService.setOutline(rootBookmark, currentFilePath, actualDest, offset, ViewScaleType.NONE); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String autoFormat(String text) {
        return pdfOutlineService.autoFormat(text);
    }

    // --- TOC Implementation ---

    @Override
    public void generateTocPage(TocConfig config, String destFilePath) {
        checkFileOpen();
        String actualDest = destFilePath != null ? destFilePath : currentFilePath;
        Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(config.tocContent(), Method.INDENT);
        
        try {
            pdfTocPageGeneratorService.createTocPage(
                    currentFilePath,
                    actualDest,
                    config.title(),
                    config.insertPos(),
                    config.style(),
                    root,
                    config.header(),
                    config.footer(),
                    msg -> {}, // Ignore info messages for RPC
                    err -> { throw new RuntimeException(err); }
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String generateTocPreview(TocConfig config) {
        Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(config.tocContent(), Method.INDENT);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            pdfTocPageGeneratorService.createTocPagePreview(
                    config.title(),
                    config.style(),
                    root,
                    baos,
                    config.header(),
                    config.footer(),
                    msg -> {},
                    err -> { throw new RuntimeException(err); }
            );
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Page Labels Implementation ---

    @Override
    public String[] getPageLabels(String srcFilePath) {
        String path = srcFilePath != null ? srcFilePath : currentFilePath;
        if (path == null) throw new IllegalStateException("No file specified and no file open");
        try {
            return pdfPageLabelService.getPageLabels(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPageLabels(List<PageLabelRule> rules, String destFilePath) {
        checkFileOpen();
        String actualDest = destFilePath != null ? destFilePath : currentFilePath;
        List<PageLabel> finalLabels = pdfPageLabelService.convertRulesToPageLabels(rules);
        try {
            pdfPageLabelService.setPageLabels(currentFilePath, actualDest, finalLabels);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> simulatePageLabels(List<PageLabelRule> rules) {
        checkFileOpen();
        try {
            String[] existingLabels = pdfPageLabelService.getPageLabels(currentFilePath);
            int totalPages = existingLabels == null ? 0 : existingLabels.length;
            if (totalPages == 0) return Collections.emptyList();
            
            return pdfPageLabelService.simulatePageLabels(rules, totalPages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
