package com.ririv.quickoutline.api.service;

import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.model.ViewScaleType;


public interface ApiService {
    void openFile(String filePath);
    String getCurrentFilePath();
    
    // Outline
    String getOutline(int offset);
    
    // CHANGED: Returns DTO to prevent circular reference during JSON serialization
    BookmarkDto getOutlineAsBookmark(int offset); 
    
    void saveOutline(Bookmark rootBookmark, String destFilePath, int offset, ViewScaleType viewMode);
    /**
     * Opens the given text content in an external editor (e.g., VS Code).
     * @param textContent The content to be written to a temporary file and opened.
     */
    void openExternalEditor(String textContent);
    
    /**
     * Clears any active preview mode, reverting the image service to file-based images.
     */
}