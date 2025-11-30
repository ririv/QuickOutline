package com.ririv.quickoutline.api.service;

import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PageLabelRule;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ApiService {
    void openFile(String filePath);
    String getCurrentFilePath();
    
    // --- Outline (Stateful Operations) ---
    
    String getOutline(int offset);
    
    // CHANGED: Returns DTO to prevent circular reference during JSON serialization
    BookmarkDto getOutlineAsBookmark(int offset); 
    
    void saveOutline(Bookmark rootBookmark, String destFilePath, int offset);
    
    void saveOutlineFromText(String text, String destFilePath, int offset);
    
    String autoFormat(String text);

    // --- State Synchronization (New) ---

    /**
     * Syncs text from frontend editor -> Backend State -> Returns Tree DTO for frontend preview.
     */
    BookmarkDto syncFromText(String text);

    /**
     * Syncs tree DTO from frontend drag/drop -> Backend State -> Returns Text for frontend editor.
     */
    String syncFromTree(BookmarkDto dto);

    /**
     * Updates the offset in Backend State.
     */
    void updateOffset(int offset);


    // --- TOC ---
    void generateTocPage(TocConfig config, String destFilePath);
    String generateTocPreview(TocConfig config);

    // --- Page Labels ---
    String[] getPageLabels(String srcFilePath);
    void setPageLabels(List<PageLabelRule> rules, String destFilePath);
    List<String> simulatePageLabels(List<PageLabelRule> rules);
    
    // --- Thumbnails / Metadata ---
    int getPageCount();
    String getThumbnail(int pageIndex);
    java.util.Map<Integer, String> getThumbnails(List<Integer> pageIndices);
    
    CompletableFuture<byte[]> getPreviewImageDataAsync(int pageIndex);

    // --- Utils ---
    BookmarkDto parseTextToTree(String text);
    String serializeTreeToText(Bookmark root); // Keep accepting Domain object for internal utility
}