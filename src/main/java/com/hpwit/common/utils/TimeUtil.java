package com.hpwit.common.utils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @Author huangpw
 * @Date 2026/4/9 17:16
 */
public class TimeUtil {

    /**
     * 判断开始时间是否小于等于结束时间
     */
    public static boolean isValid(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null && !start.isAfter(end);
    }

    /**
     * 判断 target 是否在 start ~ end 区间内（包含边界）
     */
    public static boolean isInRange(LocalDateTime target,
                                    LocalDateTime start,
                                    LocalDateTime end) {

        if (target == null || start == null || end == null) {
            return false;
        }

        return (target.isEqual(start) || target.isAfter(start))
                && (target.isEqual(end) || target.isBefore(end));
    }

    /**
     * 计算两个时间相差秒数（可能为负数）
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Duration.between(start, end).getSeconds();
    }

    /**
     * 计算两个时间相差秒数（绝对值）
     */
    public static long betweenSecondsAbs(LocalDateTime start, LocalDateTime end) {
        return Math.abs(betweenSeconds(start, end));
    }
}
