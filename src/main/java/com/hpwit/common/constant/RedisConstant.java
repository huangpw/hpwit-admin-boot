package com.hpwit.common.constant;

/**
 * Redis 常量
 *
 * <p>所有 key 模板均使用 {@code {}} 占位符，调用方通过 {@link cn.hutool.core.util.StrUtil#format} 注入实际值。</p>
 *
 * @author huangpw
 * @date 2026/1/15
 */
public interface RedisConstant {

    /**
     * 限流相关键
     */
    interface RateLimiter {
        String IP = "rate_limiter:ip:{}"; // IP 限流（示例：rate_limiter:ip:192.168.1.1）
    }

    /**
     * 分布式锁相关键
     */
    interface Lock {
        String RESUBMIT = "lock:resubmit:{}:{}"; // 防重复提交（示例：lock:resubmit:userIdentifier:requestIdentifier）
    }

    /**
     * 认证模块
     */
    interface Auth {
        String ACCESS_TOKEN_USER = "auth:token:access:{}";        // accessToken -> OnlineUser
        String REFRESH_TOKEN_USER = "auth:token:refresh:{}";      // refreshToken -> OnlineUser
        String USER_ACCESS_TOKEN = "auth:user:access:{}";         // userId -> accessToken
        String USER_REFRESH_TOKEN = "auth:user:refresh:{}";       // userId -> refreshToken
        String BLACKLIST_TOKEN = "auth:token:blacklist:{}";       // 黑名单 Token
        String USER_SECURITY_VERSION = "auth:user:security_version:{}"; // 按用户失效历史 JWT
    }

    /**
     * 验证码模块
     *
     * <p>每种验证码对应不同的 API 端点和 Redis key，互不影响：</p>
     * <table border="1">
     *   <caption>验证码类型 → 接口 → Redis key</caption>
     *   <tr><th>类型</th><th>API 端点</th><th>Redis key 模板</th></tr>
     *   <tr><td>图形验证码</td><td>GET /v1/auth/captcha</td><td>{@link Captcha#IMAGE_CODE}</td></tr>
     *   <tr><td>登录短信验证码</td><td>GET /v1/auth/sms/login-code</td><td>{@link Captcha#SMS_LOGIN_CODE}</td></tr>
     *   <tr><td>注册短信验证码</td><td>GET /v1/auth/sms/register-code</td><td>{@link Captcha#SMS_REGISTER_CODE}</td></tr>
     *   <tr><td>换绑手机验证码</td><td>GET /v1/auth/sms/mobile-code</td><td>{@link Captcha#MOBILE_CODE}</td></tr>
     *   <tr><td>邮箱验证码</td><td>GET /v1/auth/email/code</td><td>{@link Captcha#EMAIL_CODE}</td></tr>
     * </table>
     */
    interface Captcha {
        /** 图形验证码（4 位字符图片，看图填） */
        String IMAGE_CODE = "captcha:image:{}";

        /** 登录短信验证码（手机收） */
        String SMS_LOGIN_CODE = "captcha:sms_login:{}";

        /** 注册短信验证码（手机收） */
        String SMS_REGISTER_CODE = "captcha:sms_register:{}";

        /** 换绑手机号验证码（手机收） */
        String MOBILE_CODE = "captcha:mobile:{}";

        /** 邮箱验证码（邮箱收） */
        String EMAIL_CODE = "captcha:email:{}";
    }

    /**
     * 短信限流（防同一手机号短时间重复下发）
     */
    interface SmsLimit {
        /** 60 秒内同一手机号只能发一次，示例：{@code sms:limit:13800000000} */
        String PHONE = "sms:limit:{}";
    }

    /**
     * 系统模块
     */
    interface System {
        String CONFIG = "system:config";                 // 系统配置
        String ROLE_PERMS = "system:role:perms";         // 系统角色和权限映射

        /**
         * 字典项 options 缓存，value 为 {@code List<Option>} 的 JSON。
         * <p>缓存策略：Cache-Aside + 写时主动失效 + 版本号变更信号 + TTL 兜底。</p>
         * <p>示例：{@code system:dict:options:user_status} → [{value:"1",label:"启用"}, ...]</p>
         */
        String DICT_OPTIONS = "system:dict:options:{}";

        /**
         * 字典项版本号，每次写操作（新增/修改/删除/启停）+1。
         * <p>用途：① 强制让依赖旧版本的下游重新拉取；② 接入 Redis MONITOR / Streams 做变化审计；
         * ③ 对接 Pub/Sub 时可作为消息载荷让订阅方快速判断是否需要失效本地缓存。</p>
         * <p>示例：{@code system:dict:version:user_status} → 17</p>
         */
        String DICT_VERSION = "system:dict:version:{}";
    }
}
