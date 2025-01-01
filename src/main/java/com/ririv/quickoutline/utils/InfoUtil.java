package com.ririv.quickoutline.utils;

//import

//    http://lopica.sourceforge.net/os.html
public class InfoUtil {
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    public static boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static String getVersion() {
        return System.getProperty("app.version", "Unknown Version");
    }
}

