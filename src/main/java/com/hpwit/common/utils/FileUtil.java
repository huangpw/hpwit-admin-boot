package com.hpwit.common.utils;

/**
 * @Author huangpw
 * @Date 2026/2/27 17:37
 */
public class FileUtil {

    public static String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        }
    }
}
