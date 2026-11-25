package com.hpwit.platform.sms.service;

import com.hpwit.common.enums.SmsTypeEnum;
import com.hpwit.common.result.Result;

/**
 * 短信平台服务
 *
 * <p>所有业务模块（auth、user、system 等）通过本服务下发短信验证码，避免重复接入阿里云。</p>
 *
 * @author huangpw
 * @date 2026/9/9
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * <p>流程：60 秒限流 → 阿里云下发 → Redis 缓存（按 type 选不同前缀）。</p>
     *
     * @param phone 手机号
     * @param type  验证码用途（LOGIN / REGISTER / MOBILE），决定 Redis key 前缀
     * @return 统一结果包装
     */
    Result<?> sendCode(String phone, SmsTypeEnum type);

    /**
     * 校验短信验证码
     *
     * <p>校验成功后会从 Redis 删除 key（防复用）。</p>
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @param type  验证码用途（决定查哪个 Redis key）
     * @return 校验通过返回 success；不通过返回 failed(message)
     */
    Result<?> verifyCode(String phone, String code, SmsTypeEnum type);
}
