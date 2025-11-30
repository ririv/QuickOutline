package com.ririv.quickoutline.utils;

public class RpcUtils {

    /**
     * Safely converts an Object (Integer, Double, String, etc.) to int.
     * Returns 0 if null or invalid.
     */
    public static int getInt(Object num) {
        if (num == null) {
            return 0;
        }
        if (num instanceof Number) {
            return ((Number) num).intValue();
        }
        if (num instanceof String) {
            try {
                String s = ((String) num).trim();
                if (s.isEmpty()) return 0;
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
