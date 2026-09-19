package com.nova.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.whvcse.easycaptcha.SpecCaptcha;
import com.nova.auth.model.form.LoginForm;
import com.nova.auth.model.vo.CaptchaVo;
import com.nova.auth.model.vo.LoginVo;
import com.nova.auth.service.AuthService;
import com.nova.common.constant.RedisConstant;
import com.nova.common.constant.SysConstant;
import com.nova.common.result.Result;
import com.nova.system.converter.SysUserConverter;
import com.nova.system.model.entity.SysUser;
import com.nova.system.model.form.SysUserForm;
import com.nova.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证中心
 *
 * <p>只保留账号流程 + 图形验证码。邮箱/短信验证码都下沉到 {@code com.nova.platform.mail} /
 * {@code com.nova.platform.sms}。</p>
 *
 * @author huangpw
 * @date 2026/1/6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // ==================== 图形验证码 ====================
    private static final int CAPTCHA_WIDTH = 130;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_LENGTH = 4;
    private static final long CAPTCHA_TTL_SECONDS = 300L;

    private final StringRedisTemplate redisTemplate;
    private final SysUserService sysUserService;
    private final SysUserConverter sysUserConverter;

    // ==================== 账号流程 ====================

    @Override
    public Result<LoginVo> login(LoginForm loginForm) {
        // 1. 校验图形验证码（图形验证码存 Redis 时已统一小写，校验忽略大小写）
        if (StrUtil.isBlank(loginForm.getCaptchaKey()) || StrUtil.isBlank(loginForm.getCaptchaCode())) {
            return Result.failed("验证码不能为空");
        }
        String captchaRedisKey = StrUtil.format(RedisConstant.Captcha.IMAGE_CODE, loginForm.getCaptchaKey());
        String cachedCode = redisTemplate.opsForValue().get(captchaRedisKey);
        if (cachedCode == null) {
            return Result.failed("验证码已过期");
        }
        if (!cachedCode.equalsIgnoreCase(loginForm.getCaptchaCode())) {
            return Result.failed("验证码错误");
        }
        // 2. 走账号登录
        return sysUserService.login(loginForm);
    }

    @Override
    public Result<?> register(SysUserForm userForm) {
        SysUser entity = sysUserConverter.toEntity(userForm);
        return sysUserService.register(entity);
    }

    @Override
    public Result<?> logout() {
        StpUtil.logout();
        return Result.success(SysConstant.LOGOUT_SUCCESS);
    }

    @Override
    public Result<?> checkLogin() {
        return Result.success(StpUtil.isLogin());
    }

    // ==================== 图形验证码 ====================

    @Override
    public Result<CaptchaVo> getCaptcha() {
        SpecCaptcha captcha = new SpecCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LENGTH);
        // 调用 text() 触发实际生成；统一小写存储，校验时也用忽略大小写比对
        String code = captcha.text().toLowerCase();
        String img = captcha.toBase64();

        String key = IdUtil.fastSimpleUUID();
        String redisKey = StrUtil.format(RedisConstant.Captcha.IMAGE_CODE, key);
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        return Result.success(new CaptchaVo(key, img));
    }
}
