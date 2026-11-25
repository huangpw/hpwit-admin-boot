package com.hpwit.common.enums;

import com.hpwit.common.base.BaseEnum;
import lombok.Getter;

/**
 * 短信验证码用途
 *
 * <p>每种类型对应 {@code RedisConstant.Captcha} 里的一个独立 key 前缀，互不串味：</p>
 * <ul>
 *   <li>{@code LOGIN}     → {@code captcha:sms_login:{phone}}</li>
 *   <li>{@code REGISTER}  → {@code captcha:sms_register:{phone}}</li>
 *   <li>{@code MOBILE}    → {@code captcha:mobile:{phone}}    （换绑手机号）</li>
 * </ul>
 *
 * @author huangpw
 * @date 2026/4/1
 */
@Getter
public enum SmsTypeEnum implements BaseEnum<String> {

    /** 登录短信验证码 */
    LOGIN("login", "登录短信验证码"),

    /** 注册短信验证码 */
    REGISTER("register", "注册短信验证码"),

    /** 换绑手机号验证码 */
    MOBILE("mobile", "换绑手机号验证码");

    private final String value;
    private final String label;

    SmsTypeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
