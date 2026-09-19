package com.nova.platform.sms.service.impl;

import cn.hutool.core.util.StrUtil;
import com.nova.common.constant.RedisConstant;
import com.nova.common.enums.SmsTypeEnum;
import com.nova.common.result.Result;
import com.nova.platform.sms.service.SmsService;
import com.nova.platform.sms.util.AliyunSmsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信平台服务实现
 *
 * <p>基于阿里云 dysmsapi 实现，限流与缓存都走 Redis。</p>
 *
 * @author huangpw
 * @date 2026/9/9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    /** 同一手机号发送间隔（秒） */
    private static final long SMS_LIMIT_SECONDS = 60L;
    /** 短信验证码有效期（秒） */
    private static final long SMS_CODE_TTL_SECONDS = 300L;

    private final StringRedisTemplate redisTemplate;
    private final AliyunSmsUtil aliyunSmsUtil;

    @Override
    public Result<?> sendCode(String phone, SmsTypeEnum type) {
        if (StrUtil.isBlank(phone)) {
            return Result.failed("手机号不能为空");
        }
        if (type == null) {
            return Result.failed("短信类型不能为空");
        }

        // 1. 60 秒限流（防同一手机号短时间重复下发）
        String limitKey = StrUtil.format(RedisConstant.SmsLimit.PHONE, phone);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            return Result.failed("发送过于频繁，请稍后再试");
        }

        // 2. 生成 6 位数字验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 3. 阿里云下发
        boolean ok = aliyunSmsUtil.sendLoginCode(phone, code, type);
        if (!ok) {
            return Result.failed("短信下发失败");
        }

        // 4. 写 Redis（按 type 选不同 key 前缀，5 分钟有效）
        String codeKeyTemplate = switch (type) {
            case LOGIN -> RedisConstant.Captcha.SMS_LOGIN_CODE;
            case REGISTER -> RedisConstant.Captcha.SMS_REGISTER_CODE;
            case MOBILE -> RedisConstant.Captcha.MOBILE_CODE;
        };
        String codeKey = StrUtil.format(codeKeyTemplate, phone);
        redisTemplate.opsForValue().set(codeKey, code, SMS_CODE_TTL_SECONDS, TimeUnit.SECONDS);

        // 5. 写限流 key
        redisTemplate.opsForValue().set(limitKey, "1", SMS_LIMIT_SECONDS, TimeUnit.SECONDS);

        log.info("[SmsService.sendCode] type={}, phone={}, code={}", type, phone, code);
        return Result.success();
    }

    @Override
    public Result<?> verifyCode(String phone, String code, SmsTypeEnum type) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            return Result.failed("手机号或验证码不能为空");
        }
        if (type == null) {
            return Result.failed("短信类型不能为空");
        }

        String codeKeyTemplate = switch (type) {
            case LOGIN -> RedisConstant.Captcha.SMS_LOGIN_CODE;
            case REGISTER -> RedisConstant.Captcha.SMS_REGISTER_CODE;
            case MOBILE -> RedisConstant.Captcha.MOBILE_CODE;
        };
        String codeKey = StrUtil.format(codeKeyTemplate, phone);
        String cached = redisTemplate.opsForValue().get(codeKey);
        if (cached == null) {
            return Result.failed("验证码已过期");
        }
        if (!cached.equals(code)) {
            return Result.failed("验证码错误");
        }

        // 校验通过，删除（防复用）
        redisTemplate.delete(codeKey);
        return Result.success();
    }
}
