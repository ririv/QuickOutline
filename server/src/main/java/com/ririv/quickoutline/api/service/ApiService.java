package com.ririv.quickoutline.api.service;

import com.ririv.quickoutline.api.model.BookmarkDto;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.model.ViewScaleType;


public interface ApiService {
    void openFile(String filePath);
    String getCurrentFilePath();

}