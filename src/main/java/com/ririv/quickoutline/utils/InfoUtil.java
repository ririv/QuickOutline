package com.ririv.quickoutline.utils;

//import

//    http://lopica.sourceforge.net/os.html
public class InfoUtil {

    public static String getAppVersion() {
        return System.getProperty("app.version", "Unknown Version");
    }

    public static boolean isSandboxed() {
        String containerId = System.getenv("APP_SANDBOX_CONTAINER_ID");
        return containerId != null && !containerId.isEmpty();
    }
}

