package com.ririv.quickoutline.api.service.impl;

import com.google.gson.Gson;
import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.service.PageLabelRule;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.service.pdfpreview.PdfImageService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ApiServiceImpl implements ApiService {
    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);

    private final PdfOutlineService pdfOutlineService;
    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final PdfPageLabelService pdfPageLabelService;
    private final PdfImageService pdfImageService; // Re-introduced for diffing logic
    
    private String currentFilePath;

    public ApiServiceImpl(PdfOutlineService pdfOutlineService,
                          PdfTocPageGeneratorService pdfTocPageGeneratorService,
                          PdfPageLabelService pdfPageLabelService,
                          PdfImageService pdfImageService) { // Added PdfImageService
        this.pdfOutlineService = pdfOutlineService;
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.pdfPageLabelService = pdfPageLabelService;
        this.pdfImageService = pdfImageService;
    }

    private void checkFileOpen() {
        if (currentFilePath == null) throw new IllegalStateException("No file open");
    }

    // ... (openFile, getCurrentFilePath, outline methods are unchanged) ...

    @Override
    public void openFile(String filePath) {
        try {
            pdfOutlineService.checkOpenFile(filePath);
            this.currentFilePath = filePath;
            this.pdfImageService.clearCache(); // Clear cache on new file
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
            pdfOutlineService.setOutline(rootBookmark, currentFilePath, actualDest, offset, ViewScaleType.NONE); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOutlineFromText(String text, String destFilePath, int offset) {
        checkFileOpen();
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(text, Method.INDENT);
        saveOutline(rootBookmark, destFilePath, offset);
    }

    @Override
    public String autoFormat(String text) {
        return pdfOutlineService.autoFormat(text);
    }


    @Override
    public void generateTocPage(TocConfig config, String destFilePath) {
        // ... (this method remains the same) ...
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
                    msg -> log.info("TOC Gen msg: {}", msg), 
                    err -> { throw new RuntimeException(err); }
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String generateTocPreview(TocConfig config) {
        log.info("Generating TOC preview for title: {}", config.title());
        Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(config.tocContent(), Method.INDENT);
        
        if (root == null || root.getChildren().isEmpty()) {
            log.warn("TOC preview generation skipped: content is empty after parsing.");
            return new Gson().toJson(Collections.emptyList());
        }

        try (FastByteArrayOutputStream baos = new FastByteArrayOutputStream()) {
            log.debug("Creating in-memory preview PDF...");
            pdfTocPageGeneratorService.createTocPagePreview(
                    config.title(),
                    config.style(),
                    root,
                    baos,
                    config.header(),
                    config.footer(),
                    msg -> log.info("TOC Preview msg: {}", msg),
                    err -> { throw new RuntimeException(err); }
            );
            
            byte[] pdfBytes = baos.getBuffer();
            int size = baos.size();
            // Create a new stream with the exact size, because diffPdfToImages expects it
            FastByteArrayOutputStream finalStream = new FastByteArrayOutputStream();
            finalStream.write(pdfBytes, 0, size);

            log.debug("Preview PDF generated, size: {} bytes. Diffing images...", size);
            
            if (size == 0) {
                log.warn("Preview PDF is empty, cannot render images.");
                return new Gson().toJson(Collections.emptyList());
            }

            // Restore old logic: use PdfImageService to get diff updates
            List<PdfImageService.ImagePageUpdate> updates = pdfImageService.diffPdfToImages(finalStream);
            log.info("Found {} updated image(s) for preview.", updates.size());
            
            return new Gson().toJson(updates);
            
        } catch (Exception e) {
            log.error("Failed to generate TOC preview.", e);
            throw new RuntimeException(e);
        }
    }

    // --- New method for the web server to get image data ---
    public byte[] getPreviewImageData(int pageIndex) {
        return pdfImageService.getImageData(pageIndex);
    }


    // ... (Page Labels implementation remains the same) ...
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
}