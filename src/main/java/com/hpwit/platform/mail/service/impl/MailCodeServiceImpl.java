package com.hpwit.platform.mail.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hpwit.common.constant.RedisConstant;
import com.hpwit.common.enums.EmailTypeEnum;
import com.hpwit.common.result.Result;
import com.hpwit.platform.mail.service.MailCodeService;
import com.hpwit.platform.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮件验证码服务实现
 *
 * @author huangpw
 * @date 2026/9/9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailCodeServiceImpl implements MailCodeService {

    /** 邮件验证码有效期（秒） */
    private static final long EMAIL_CODE_TTL_SECONDS = 300L;

    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    @Override
    public Result<?> sendCode(String email, EmailTypeEnum type) {
        if (StrUtil.isBlank(email)) {
            return Result.failed("邮箱不能为空");
        }
        EmailTypeEnum effectiveType = type == null ? EmailTypeEnum.LOGIN : type;

        // 1. 生成 6 位数字验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 2. 发送邮件（subject 自动按 type 拼）
        String subject = String.format("[ZyxhitAdmin] %s验证码", effectiveType.getLabel());
        mailService.sendMail(email, subject,
                "您的验证码为：" + code + "，请在 5 分钟内使用");

        // 3. 缓存到 Redis（5 分钟有效，校验后删除）
        String redisKey = StrUtil.format(RedisConstant.Captcha.EMAIL_CODE, email);
        redisTemplate.opsForValue().set(redisKey, code, EMAIL_CODE_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("[MailCodeService.sendCode] type={}, email={}, code={}", effectiveType, email, code);
        return Result.success();
    }

    @Override
    public Result<?> verifyCode(String email, String code) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(code)) {
            return Result.failed("邮箱或验证码不能为空");
        }

        String redisKey = StrUtil.format(RedisConstant.Captcha.EMAIL_CODE, email);
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            return Result.failed("验证码已过期");
        }
        if (!cached.equals(code)) {
            return Result.failed("验证码错误");
        }

        // 校验通过，删除（防复用）
        redisTemplate.delete(redisKey);
        return Result.success();
    }
}
