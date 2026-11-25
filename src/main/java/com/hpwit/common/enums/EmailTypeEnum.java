package com.hpwit.common.enums;

import com.hpwit.common.base.BaseEnum;
import lombok.Getter;

/**
 * 邮箱验证码用途
 *
 * <p>实际存 Redis 时统一用 {@code RedisConstant.Captcha.EMAIL_CODE}（一个 key 即可），
 * 这里仅用于日志/审计/未来按类型分模板。</p>
 *
 * @author huangpw
 * @date 2026/9/9
 */
@Getter
public enum EmailTypeEnum implements BaseEnum<String> {

    /** 登录验证码 */
    LOGIN("login", "登录验证码"),

    /** 注册验证码 */
    REGISTER("register", "注册验证码"),

    /** 找回密码验证码 */
    RESET_PASSWORD("reset_password", "找回密码验证码"),

    /** 换绑邮箱验证码 */
    BIND("bind", "换绑邮箱验证码");

    private final String value;
    private final String label;

    EmailTypeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
