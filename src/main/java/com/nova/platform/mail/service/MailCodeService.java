package com.nova.platform.mail.service;

import com.nova.common.enums.EmailTypeEnum;
import com.nova.common.result.Result;

/**
 * 邮件验证码平台服务
 *
 * <p>基于 {@link MailService}（底层邮件下发）之上，封装"生成验证码 + 写 Redis + 邮件下发"完整流程。
 * 所有业务模块（auth、user、system 等）通过本服务使用邮件验证码，避免重复实现。</p>
 *
 * @author huangpw
 * @date 2026/9/9
 */
public interface MailCodeService {

    /**
     * 发送邮件验证码
     *
     * <p>流程：生成 6 位数字 → 写 Redis（{@code RedisConstant.Captcha.EMAIL_CODE}，5 分钟）→ 调 {@link MailService} 下发。</p>
     *
     * @param email 收件邮箱
     * @param type  验证码用途（LOGIN/REGISTER/RESET_PASSWORD/BIND），仅用于日志和邮件主题
     * @return 统一结果包装
     */
    Result<?> sendCode(String email, EmailTypeEnum type);

    /**
     * 校验邮件验证码
     *
     * <p>校验成功后会从 Redis 删除 key（防复用）。</p>
     *
     * @param email 收件邮箱
     * @param code  用户输入的验证码
     * @return 校验通过返回 success；不通过返回 failed(message)
     */
    Result<?> verifyCode(String email, String code);
}
