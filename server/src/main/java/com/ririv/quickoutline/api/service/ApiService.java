package com.ririv.quickoutline.api.service;

import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.model.ViewScaleType;


public interface ApiService {
    void openFile(String filePath);
    String getCurrentFilePath();
    
    /**
     * Opens the given text content in an external editor (e.g., VS Code).
     * @param textContent The content to be written to a temporary file and opened.
     */
    void openExternalEditor(String textContent);
}