package com.ririv.quickoutline.service;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class PdfTocPageGeneratorService {
    private final TocPageGenerator tocPageGenerator;

    @Inject
    public PdfTocPageGeneratorService(TocPageGenerator tocPageGenerator) {
        this.tocPageGenerator = tocPageGenerator;
    }

    public void createTocPage(String srcFilePath, String destFilePath, String title, int insertPos,
                              PageLabelNumberingStyle style, List<Bookmark> bookmarks,
                              Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        tocPageGenerator.generateAndInsertToc(srcFilePath, destFilePath, title, insertPos, style, bookmarks, onMessage, onError);
    }

    public void createTocPagePreview(String title, PageLabelNumberingStyle style, List<Bookmark> bookmarks,
                                     OutputStream outputStream,
                                     Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        tocPageGenerator.generateTocPagePreview(title, style, bookmarks, outputStream, onMessage, onError);
    }
}
