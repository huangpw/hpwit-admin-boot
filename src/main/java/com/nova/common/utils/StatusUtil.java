package com.nova.common.utils;

/**
 * @Author huangpw
 * @Date 2025/12/9 9:28
 */
public class StatusUtil {

    /**
     * 判断状态是否启用
     */
    public static boolean isStatus(Integer status) {
        return status != null && status == 1;
    }

    /**
     * 判断是否开始新增
     */
    public static boolean isAddFlag(Integer addFlag) {
        return addFlag != null && addFlag == 1;
    }

    public static boolean isUpdateFlag(Integer updateFlag) {
        return updateFlag != null && updateFlag == 1;
    }
}
