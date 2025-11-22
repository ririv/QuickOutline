package com.ririv.quickoutline.pdfProcess;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

public interface TocPageGenerator {
    void generateAndInsertToc(String srcFilePath, String destFilePath, String title, int insertPos,
                              PageLabelNumberingStyle style, Bookmark rootBookmark,
                              Consumer<String> onMessage, Consumer<String> onError) throws IOException;

    void generateTocPagePreview(String title, PageLabelNumberingStyle style, Bookmark rootBookmark,
                                OutputStream outputStream,
                                Consumer<String> onMessage, Consumer<String> onError) throws IOException;
}
