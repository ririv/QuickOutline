package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;

public class PdfTocPageGeneratorService {
    private final TocPageGenerator tocPageGenerator = new iTextTocPageGenerator();

    public void createTocPage(String srcFilePath, String destFilePath, String title, int insertPos, PageLabelNumberingStyle style, List<Bookmark> bookmarks) throws IOException {
        tocPageGenerator.generateAndInsertToc(srcFilePath, destFilePath, title, insertPos, style, bookmarks);
    }

    public void createTocPagePreview(String title, PageLabelNumberingStyle style, List<Bookmark> bookmarks, java.io.OutputStream outputStream) throws IOException {
        tocPageGenerator.generateTocPagePreview(title, style, bookmarks, outputStream);
    }
}
