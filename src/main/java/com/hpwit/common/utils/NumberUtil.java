package com.hpwit.common.utils;

/**
 * @Author huangpw
 * @Date 2025/11/27 15:37
 */
public class NumberUtil {

    /**
     * 生成Rfid人员编号
     */
    public static String generatePersonalCode(Long maxNumber) {

        if (maxNumber == null) {
            maxNumber = 0L;
        }

        // 下一个编号
        long nextNumber = maxNumber + 1;

        // 计算前缀 + 动态补零
        String prefix = "PCN";
        int totalLength = 12; // 整体长度固定12位

        String numberStr = String.valueOf(nextNumber);
        int zeroCount = totalLength - prefix.length() - numberStr.length(); // 计算需要补0的个数

        StringBuilder code = new StringBuilder(prefix);
        for (int i = 0; i < zeroCount; i++) {
            code.append('0');
        }
        code.append(numberStr);

        return code.toString();
    }

    /**
     * 截取子节点的父节点展位号
     * @param code 当前展位号
     */
    public static String getBoothNo(String code) {
        if (code == null) {
            return null;
        }
        int index = code.indexOf("-");
        if (index == -1) {
            return code; // 没有 - 直接返回
        }
        return code.substring(0, index);
    }

    public static String generateEntrantsNo(Long maxNumber) {
        String prefix = "PCN";
        return prefix + (maxNumber + 1);
    }
}
