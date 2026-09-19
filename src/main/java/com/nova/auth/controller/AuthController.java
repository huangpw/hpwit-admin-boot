package com.nova.auth.controller;

import com.nova.auth.model.form.LoginForm;
import com.nova.auth.model.vo.CaptchaVo;
import com.nova.auth.model.vo.LoginVo;
import com.nova.auth.service.AuthService;
import com.nova.common.enums.LogModuleEnum;
import com.nova.common.result.Result;
import com.nova.core.annotation.Log;
import com.nova.system.model.form.SysUserForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证中心
 *
 * <p>本控制器现在只管：账号登录/注册/登出/校验 + 图形验证码。</p>
 * <ul>
 *   <li>邮箱验证码 → {@link com.nova.platform.mail.controller.MailController}</li>
 *   <li>短信验证码 → {@link com.nova.platform.sms.controller.SmsController}</li>
 * </ul>
 *
 * @author huangpw
 * @date 2026/1/6
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ==================== 图形验证码 ====================

    @Operation(summary = "获取图形验证码（4位字符）")
    @GetMapping("/captcha")
    public Result<CaptchaVo> captcha() {
        return authService.getCaptcha();
    }

    // ==================== 账号流程 ====================

    @Operation(summary = "账号登录")
    @PostMapping("/login")
    @Log(value = "登录", module = LogModuleEnum.LOGIN)
    public Result<LoginVo> login(@RequestBody LoginForm loginForm) {
        return authService.login(loginForm);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody SysUserForm userForm) {
        return authService.register(userForm);
    }

    @Operation(summary = "退出登录")
    @GetMapping("/logout")
    public Result<?> logout() {
        return authService.logout();
    }

    @Operation(summary = "检查登录状态")
    @GetMapping("/check/login")
    public Result<?> checkLogin() {
        return authService.checkLogin();
    }
}
