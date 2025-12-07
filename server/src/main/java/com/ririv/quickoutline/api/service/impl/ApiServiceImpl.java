package com.ririv.quickoutline.api.service.impl;

import com.google.gson.Gson;
import com.ririv.quickoutline.api.state.ApiBookmarkState;
import com.ririv.quickoutline.api.state.CurrentFileState;
import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.api.service.ApiService;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.service.*;
import com.ririv.quickoutline.service.pdfpreview.FileImageService;
import com.ririv.quickoutline.service.pdfpreview.ImagePageUpdate;
import com.ririv.quickoutline.service.pdfpreview.PreviewImageService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.api.WebSocketSessionManager;
import jakarta.inject.Inject;

public class ApiServiceImpl implements ApiService {
    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);

    private final PdfOutlineService pdfOutlineService;

    private final PdfCheckService pdfCheckService;
    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final PdfPageLabelService pdfPageLabelService;
    private final FileImageService fileImageService;
    private final PreviewImageService previewImageService;
    private final ApiBookmarkState apiBookmarkState;
    private final CurrentFileState currentFileState;
    private final SyncWithExternalEditorService syncService;
    private final WebSocketSessionManager sessionManager;

    @Inject
    public ApiServiceImpl(PdfCheckService pdfCheckService,
                          PdfOutlineService pdfOutlineService,
                          PdfTocPageGeneratorService pdfTocPageGeneratorService,
                          PdfPageLabelService pdfPageLabelService,
                          FileImageService fileImageService,
                          PreviewImageService previewImageService,
                          ApiBookmarkState apiBookmarkState,
                          CurrentFileState currentFileState,
                          SyncWithExternalEditorService syncService,
                          WebSocketSessionManager sessionManager) {
        this.pdfCheckService = pdfCheckService;
        this.pdfOutlineService = pdfOutlineService;
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.pdfPageLabelService = pdfPageLabelService;
        this.fileImageService = fileImageService;
        this.previewImageService = previewImageService;
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
            
            // Delegate to FileImageService and clear preview
            previewImageService.clear();
            this.fileImageService.openFile(new File(filePath));
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
    public BookmarkDto syncFromText(String text) {
        // Parse Text -> Domain
        Bookmark root = pdfOutlineService.convertTextToBookmarkTreeByMethod(text, Method.INDENT);

        // Update State
        apiBookmarkState.setRootBookmark(root);

        // Return DTO for Frontend Tree
        return BookmarkDto.fromDomain(root);
    }

    @Override
    public String syncFromTree(BookmarkDto dto) {
        // DTO -> Domain
        Bookmark root = dto.toDomain();

        // Update State
        apiBookmarkState.setRootBookmark(root);

        // Return Text for Frontend Editor
        return root.toOutlineString();
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
    public void saveOutlineFromText(String text, String destFilePath, int offset, ViewScaleType viewMode) {
        checkFileOpen();

        // Update state first
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(text, Method.INDENT);
        apiBookmarkState.setRootBookmark(rootBookmark);
        apiBookmarkState.setOffset(offset);

        // Then save
        saveOutline(rootBookmark, destFilePath, offset, viewMode);
    }

    @Override
    public String autoFormat(String text) {
        return pdfOutlineService.autoFormat(text);
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
                    config.style(),
                    root,
                    config.header(),
                    config.footer(),
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
                    err -> {
                        throw new RuntimeException(err);
                    }
            );

            byte[] pdfBytes = baos.getBuffer();
            int size = baos.size();
            FastByteArrayOutputStream finalStream = new FastByteArrayOutputStream();
            finalStream.write(pdfBytes, 0, size);

            log.debug("Preview PDF generated, size: {} bytes. Diffing images...", size);

            if (size == 0) {
                log.warn("Preview PDF is empty, cannot render images.");
                return new Gson().toJson(Collections.emptyList());
            }

            // Delegate to PreviewImageService
            List<ImagePageUpdate> updates = previewImageService.updatePreview(finalStream);
            log.info("Found {} updated image(s) for preview.", updates.size());

            return new Gson().toJson(updates);

        } catch (Exception e) {
            log.error("Failed to generate TOC preview.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<byte[]> getFileImageAsync(int pageIndex) {
        return fileImageService.getImage(pageIndex);
    }

    @Override
    public CompletableFuture<byte[]> getFileThumbnailAsync(int pageIndex) {
        return fileImageService.getThumbnail(pageIndex);
    }

    @Override
    public CompletableFuture<byte[]> getPreviewImageAsync(int pageIndex) {
        return previewImageService.getImage(pageIndex);
    }

    @Override
    public String getThumbnail(int pageIndex) {
        // Thumbnails (Base64) are typically for the file itself
        byte[] data = getFileImageAsync(pageIndex).join();
        if (data == null || data.length == 0) return null;
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    @Override
    public Map<Integer, String> getThumbnails(List<Integer> pageIndices) {
        checkFileOpen();
        Map<Integer, String> result = new HashMap<>();
        for (Integer index : pageIndices) {
            String base64 = getThumbnail(index);
            if (base64 != null) {
                result.put(index, base64);
            }
        }
        return result;
    }

    @Override
    public String[] getPageLabels(String srcFilePath) {
        String path = srcFilePath != null ? srcFilePath : currentFileState.getFilePath();
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
        String actualDest = resolveDestFilePath(destFilePath);
        List<PageLabel> finalLabels = pdfPageLabelService.convertRulesToPageLabels(rules);
        try {
            pdfPageLabelService.setPageLabels(currentFileState.getFilePath(), actualDest, finalLabels);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> simulatePageLabels(List<PageLabelRule> rules) {
        try {
            String[] existingLabels = pdfPageLabelService.getPageLabels(currentFileState.getFilePath());
            int totalPages = existingLabels == null ? 0 : existingLabels.length;
            if (totalPages == 0) return Collections.emptyList();

            return pdfPageLabelService.simulatePageLabels(rules, totalPages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getPageCount() {
        // Delegate to the appropriate service
        if (previewImageService.isActive()) {
            return previewImageService.getTotalPages();
        }
        return fileImageService.getTotalPages();
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
    
    @Override
    public void clearPreview() {
        previewImageService.clear();
    }
}
