package com.ririv.quickoutline.service;

import com.ririv.quickoutline.exception.EncryptedPdfException;

import java.io.File;
import java.io.IOException;

public class FileService {

    private static FileService instance;

    private File srcFile;
    private File destFile;


    // Using PdfOutlineService to check file validity
    private final PdfOutlineService pdfOutlineService = new PdfOutlineService();

    private FileService() {
    }

    public static synchronized FileService getInstance() {
        if (instance == null) {
            instance = new FileService();
        }
        return instance;
    }

    public File getSrcFile() {
        return srcFile;
    }

    public File getDestFile() {
        return destFile;
    }

    public void setSrcFile(File file) throws IOException, EncryptedPdfException {
        if (file == null) {
            clear();
            return;
        }

        // This check logic was in MainController's openFile
        pdfOutlineService.checkOpenFile(file.getPath());

        this.srcFile = file;

        calculateDestFile();
    }

    private void calculateDestFile() {
        if (srcFile == null) {
            destFile = null;
            return;
        }
        String srcPath = srcFile.getPath().replaceAll("\\\\", "/");
        String srcFileName = srcPath.substring(srcPath.lastIndexOf("/") + 1);
        String ext = srcFileName.substring(srcFileName.lastIndexOf("."));
        String destPath = srcPath.substring(0, srcPath.lastIndexOf(srcFileName)) +
                          srcFileName.substring(0, srcFileName.lastIndexOf(".")) +
                          "_含目录" + ext;

        this.destFile = new File(destPath);
    }

    public void clear() {
        this.srcFile = null;
        this.destFile = null;
    }
}
