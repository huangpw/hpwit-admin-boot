package com.hpwit.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import com.hpwit.common.property.AliyunSmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信配置类
 *
 * <p>注册阿里云 dysmsapi 客户端 Bean，被 {@code com.hpwit.platform.sms.util.AliyunSmsUtil} 注入使用。
 * 放到顶层 {@code config/} 包是为了和 CaffeineConfig / SaTokenConfig / DruidConfig 等
 * 第三方 SDK / 跨切面配置保持一致风格。</p>
 *
 * @author huangpw
 * @date 2026/4/1
 */
@Configuration
public class AliyunSmsConfig {

    @Bean
    public Client smsClient(AliyunSmsProperties prop) throws Exception {
        Config config = new Config()
                .setAccessKeyId(prop.getAccessKeyId())
                .setAccessKeySecret(prop.getAccessKeySecret());

        config.endpoint = "dysmsapi.aliyuncs.com";
        return new Client(config);
    }
}
