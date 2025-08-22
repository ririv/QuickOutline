package com.ririv.quickoutline.service;

import com.google.inject.Inject;
import com.ririv.quickoutline.model.Bookmark;

import java.io.IOException;
import java.util.List;

public class TocService {

    private final TocPageGenerator tocPageGenerator;

    @Inject
    public TocService(TocPageGenerator tocPageGenerator) {
        this.tocPageGenerator = tocPageGenerator;
    }

    public void createTocPage(String srcFilePath, String destFilePath, List<Bookmark> bookmarks) throws IOException {
        tocPageGenerator.generateAndInsertToc(srcFilePath, destFilePath, bookmarks);
    }
}
