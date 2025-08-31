package com.ririv.quickoutline.service;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;

import java.io.IOException;
import java.util.List;

public class PdfTocPageGeneratorService {

    private final TocPageGenerator tocPageGenerator;

    public PdfTocPageGeneratorService(TocPageGenerator tocPageGenerator) {
        this.tocPageGenerator = tocPageGenerator;
    }

    public void createTocPage(String srcFilePath, String destFilePath, List<Bookmark> bookmarks) throws IOException {
        tocPageGenerator.generateAndInsertToc(srcFilePath, destFilePath, bookmarks);
    }
}
