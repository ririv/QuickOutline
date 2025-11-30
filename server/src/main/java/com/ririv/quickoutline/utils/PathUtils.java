package com.ririv.quickoutline.utils;

public class PathUtils {
    public static String getUserHomePath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            throw new RuntimeException("无法获取用户主目录路径");
        }
        return userHome;
    }
}
