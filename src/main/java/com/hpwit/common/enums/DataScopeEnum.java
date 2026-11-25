package com.hpwit.common.enums;

import com.hpwit.common.base.BaseEnum;
import lombok.Getter;

/**
 * 数据权限范围
 * @Author huangpw
 * @Date 2026/1/12 16:41
 */
@Getter
public enum DataScopeEnum implements BaseEnum<Integer> {

    /**
     * value 越小，数据权限范围越大
     */
    ALL(1, "所有数据"),
    DEPT_AND_SUB(2, "部门及子部门数据"),
    DEPT(3, "本部门数据"),
    SELF(4, "本人数据");

    private final Integer value;
    private final String label;

    DataScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据 value 获取枚举
     */
    public static DataScopeEnum of(Integer value) {
        if (value == null) {
            return null;
        }
        for (DataScopeEnum scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("未知的数据权限类型: " + value);
    }
}
