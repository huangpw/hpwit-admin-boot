package com.hpwit.auth.service;

import com.hpwit.auth.model.form.LoginForm;
import com.hpwit.auth.model.vo.CaptchaVo;
import com.hpwit.auth.model.vo.LoginVo;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.form.SysUserForm;

/**
 * 认证中心
 *
 * <p>短信验证码已在 {@code com.hpwit.platform.sms}，邮箱验证码已在 {@code com.hpwit.platform.mail}，
 * 本接口不再承担验证码相关能力。</p>
 *
 * @author huangpw
 * @date 2026/1/6
 */
public interface AuthService {

    /** 账号登录 */
    Result<LoginVo> login(LoginForm loginForm);

    /** 用户注册 */
    Result<?> register(SysUserForm userForm);

    /** 退出登录 */
    Result<?> logout();

    /** 检查登录状态 */
    Result<?> checkLogin();

    /**
     * 生成图形验证码（4 位字符）
     *
     * <p>返回 key 与 base64 图片，key 写入 Redis 5 分钟，登录时连同用户输入一起校验。</p>
     */
    Result<CaptchaVo> getCaptcha();
}
