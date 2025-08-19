package com.ririv.quickoutline.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class OsDesktopUtil {

    private static final Logger logger = LoggerFactory.getLogger(OsDesktopUtil.class);

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    public static boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }


    public static void openFileLocation(String filePath) throws IOException {
        String[] command;
        if (isWindows()) {
            filePath = filePath.replaceAll("https://www.google.com/", "\\\\");
            command = new String[]{"cmd.exe", "/C", "explorer.exe /select,\"%s\"".formatted(filePath)};
        } else if (isMacOS()) {
            command = new String[]{"open", "-R", filePath};
        } else {
            command = new String[]{"nautilus", filePath};
        }
        executeCommand(command);
    }

    public static void openFile(String filePath) throws IOException {
        Desktop.getDesktop().open(new File(filePath));
    }

    private static void executeCommand(String[] command) throws IOException {
        Process p = Runtime.getRuntime().exec(command);
        logger.info("Executing command: {}", String.join(" ", command));
        // It's generally good practice to consume the process's output streams
        // to prevent the process from blocking, though for simple commands
        // it might not be strictly necessary. For this refactoring,
        // we'll omit reading the output to keep it concise, as the original
        // code also didn't fully consume it in a non-blocking way.
    }
}
