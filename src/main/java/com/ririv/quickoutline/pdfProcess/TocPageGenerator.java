package com.ririv.quickoutline.pdfProcess;

import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.model.Bookmark;

import java.io.IOException;
import java.util.List;

public interface TocPageGenerator {
    void generateAndInsertToc(String srcFilePath, String destFilePath, String title, int insertPos, PageLabelNumberingStyle style, List<Bookmark> bookmarks) throws IOException;
    void generateTocPagePreview(String title, PageLabelNumberingStyle style, List<Bookmark> bookmarks, java.io.OutputStream outputStream) throws IOException;
}
