package com.ririv.quickoutline.api.service;

import com.ririv.quickoutline.api.model.TocConfig;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PageLabelRule;

import java.util.List;

public interface ApiService {
    void openFile(String filePath);
    String getCurrentFilePath();
    
    // Outline
    String getOutline(int offset);
    Bookmark getOutlineAsBookmark(int offset);
    void saveOutline(Bookmark rootBookmark, String destFilePath, int offset);
    String autoFormat(String text);

    // TOC
    void generateTocPage(TocConfig config, String destFilePath);
    String generateTocPreview(TocConfig config); // Returns Base64 PDF or Error

    // Page Labels
    String[] getPageLabels(String srcFilePath);
    void setPageLabels(List<PageLabelRule> rules, String destFilePath);
    List<String> simulatePageLabels(List<PageLabelRule> rules);
}