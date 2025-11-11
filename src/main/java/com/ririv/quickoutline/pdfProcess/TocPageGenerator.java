package com.ririv.quickoutline.pdfProcess;

import com.ririv.quickoutline.model.Bookmark;

import java.io.IOException;
import java.util.List;

public interface TocPageGenerator {
    void generateAndInsertToc(String srcFilePath, String destFilePath, String title, List<Bookmark> bookmarks) throws IOException;
}
