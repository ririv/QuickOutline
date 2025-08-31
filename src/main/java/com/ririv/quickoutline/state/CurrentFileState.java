package com.ririv.quickoutline.state;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.service.PdfOutlineService;

import java.io.IOException;
import java.nio.file.Path;

public class CurrentFileState {

    private Path srcFile;
    private Path destFile;

    private final PdfOutlineService pdfOutlineService;

    public CurrentFileState(PdfOutlineService pdfOutlineService) {
        this.pdfOutlineService = pdfOutlineService;
    }

    public Path getSrcFile() {
        return srcFile;
    }

    public Path getDestFile() {
        return destFile;
    }

    public void setSrcFile(Path file) throws IOException, EncryptedPdfException, com.itextpdf.io.exceptions.IOException {
        if (file == null) {
            clear();
            return;
        }
        // Centralized validation
        pdfOutlineService.checkOpenFile(file.toString());
        this.srcFile = file;
        this.destFile = calculateDestFilePath(file);
    }

    public Path calculateDestFilePath(Path srcFilePath) {
        if (srcFilePath == null) {
            return null;
        }
        String srcFileName = srcFilePath.getFileName().toString();
        int dotIndex = srcFileName.lastIndexOf(".");
        String nameWithoutExt = (dotIndex == -1) ? srcFileName : srcFileName.substring(0, dotIndex);
        String ext = (dotIndex == -1) ? "" : srcFileName.substring(dotIndex);

        Path parentDir = srcFilePath.getParent();
        String destFileName = nameWithoutExt + "_new" + ext;
        return parentDir.resolve(destFileName);
    }

    public void clear() {
        this.srcFile = null;
        this.destFile = null;
    }
}
