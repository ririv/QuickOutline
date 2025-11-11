package com.ririv.quickoutline.service;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator;

import java.io.IOException;
import java.util.List;

public class PdfTocPageGeneratorService {

    private final TocPageGenerator tocPageGenerator = new iTextTocPageGenerator();

    public void createTocPage(String srcFilePath, String destFilePath, String title, List<Bookmark> bookmarks) throws IOException {
        tocPageGenerator.generateAndInsertToc(srcFilePath, destFilePath, title, bookmarks);
    }
}
