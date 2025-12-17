package com.ririv.quickoutline.api.service;

import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PageLabelRule;
import com.ririv.quickoutline.model.ViewScaleType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ApiService {
    void openFile(String filePath);
    String getCurrentFilePath();
    
    // Outline
    String getOutline(int offset);
    
    // CHANGED: Returns DTO to prevent circular reference during JSON serialization
    BookmarkDto getOutlineAsBookmark(int offset); 
    
    void saveOutline(Bookmark rootBookmark, String destFilePath, int offset, ViewScaleType viewMode);
    
    void saveOutlineFromText(String text, String destFilePath, int offset, ViewScaleType viewMode);
    
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

    // --- Page Labels ---
    String[] getPageLabels(String srcFilePath);
    void setPageLabels(List<PageLabelRule> rules, String destFilePath);
    List<String> simulatePageLabels(List<PageLabelRule> rules);

    // --- Utils ---
    BookmarkDto parseTextToTree(String text);
    String serializeTreeToText(Bookmark root); // Keep accepting Domain object for internal utility

    /**
     * Opens the given text content in an external editor (e.g., VS Code).
     * @param textContent The content to be written to a temporary file and opened.
     */
    void openExternalEditor(String textContent);
    
    /**
     * Clears any active preview mode, reverting the image service to file-based images.
     */
}