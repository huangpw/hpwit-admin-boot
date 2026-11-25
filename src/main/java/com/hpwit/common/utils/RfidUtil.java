package com.hpwit.common.utils;

/**
 * @Author huangpw
 * @Date 2026/3/19 15:42
 */
public class RfidUtil {

    /**
     * 获取编码第一个 - 之前的内容
     * @param code 原始编码
     * @return 处理后的编码
     */
    public static String getMainCode(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        int index = code.indexOf("-");
        if (index == -1) {
            return code;
        }

        return code.substring(0, index);
    }

}
