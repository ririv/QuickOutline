package com.ririv.quickoutline.service;

import com.ririv.quickoutline.model.Bookmark;

import java.io.IOException;
import java.util.List;

public interface TocPageGenerator {
    void generateAndInsertToc(String srcFilePath, String destFilePath, List<Bookmark> bookmarks) throws IOException;
}
