package com.hpwit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.hpwit.common.base.BaseEnum;
import lombok.Getter;

/**
 * 菜单类型枚举
 *
 * @author Ray.Hao
 * @since 2022/4/23 9:36
 */
@Getter
public enum MenuTypeEnum implements BaseEnum<Integer> {

    NULL(0, null),
    MENU(1, "page"),
    CATALOG(2, "dir"),
    LINK(3, "link"),
    BUTTON(4, "button"),

   UNKNOWN(5, "unknown");

    //  Mybatis-Plus 提供注解表示插入数据库时插入该值
    @EnumValue
    private final Integer value;

    // @JsonValue //  表示对枚举序列化时返回此字段
    private final String label;

    MenuTypeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }


    /**
     * 根据 value 获取枚举实例
     */
    public static MenuTypeEnum values(Integer value) {
        for (MenuTypeEnum type : MenuTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return UNKNOWN; // 如果没有匹配的返回 UNKNOWN
    }

    /**
     * 根据 value 获取 label
     */
    public static String getLabel(Integer value) {
        MenuTypeEnum type = values(value);
        return type.label;
    }

}
