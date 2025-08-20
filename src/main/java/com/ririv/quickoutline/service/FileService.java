package com.ririv.quickoutline.service;

import com.ririv.quickoutline.exception.EncryptedPdfException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileService {

    private static FileService instance;

    private Path srcFile;
    private Path destFile;

    private FileService() {
    }

    public static synchronized FileService getInstance() {
        if (instance == null) {
            instance = new FileService();
        }
        return instance;
    }

    public Path getSrcFile() {
        return srcFile;
    }

    public Path getDestFile() {
        return destFile;
    }

    public void setSrcFile(Path file) {
        if (file == null) {
            clear();
            return;
        }
        this.srcFile = file;
        calculateDestFile();
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
        String destFileName = nameWithoutExt + "_含目录" + ext;
        return parentDir.resolve(destFileName);
    }

    private void calculateDestFile() {
        this.destFile = calculateDestFilePath(this.srcFile);
    }

    public void clear() {
        this.srcFile = null;
        this.destFile = null;
    }
    

    public static String getUserHomePath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            throw new RuntimeException("无法获取用户主目录路径");
        }
        return userHome;
    }
}
