package com.nova.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * SaToken配置
 * @Author huangpw
 * @Date 2025/11/17 17:45
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** 请求接口白名单（按需增减） */
    List<String> whiteList = Arrays.asList(
            // 登录注册接口
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/logout",
            "/v1/auth/check/login",
            // 图形验证码（公开）
            "/v1/auth/captcha",
            // 邮件平台（公开，含验证码 + 通用发送）
            "/v1/mail/**",
            // 短信平台（公开）
            "/v1/sms/**",

            // 放行 Druid 监控
            "/druid/**",

            // Knife4j / springdoc 接口文档相关路径
            "/swagger-ui.html",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",

            // 平台静态资源前缀
            "/files/**",
            "/qr/**",
            "/mail/**"
    );

    /** 注册拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                //.excludePathPatterns("/user/login")
                .excludePathPatterns(whiteList)
                // 开启异步支持（解决之前的AsyncRequestNotUsableException）
                .order(0);
    }

    /** 配置 RedisTemplate 序列化 */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 序列化：String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化：Jackson
        GenericJackson2JsonRedisSerializer jacksonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;

    }

    // 将自定义的Redis Dao注册为Sa-Token的默认数据层
    //@Bean
    //public SaTokenDao saTokenDao(SaTokenDaoForRedisTemplate saTokenDaoForRedisTemplate) {
    //    return saTokenDaoForRedisTemplate;
    //}
    // 唯一注册SaTokenDao Bean，避免冲突

    /** 自定义 Sa-Token Dao，标记为 @Primary 避免冲突 */
    //@Bean
    //@Primary
    //public SaTokenDao saTokenDao(RedisConnectionFactory connectionFactory) {
    //    SaTokenDaoForRedisTemplate dao = new SaTokenDaoForRedisTemplate();
    //    // 手动调用init方法初始化RedisTemplate
    //    dao.init(connectionFactory);
    //    return dao;
    //}\

    @Bean
    @Primary  // 自定义 Sa-Token Dao，标记为 @Primary 避免冲突 */
    public SaTokenDao saTokenDao(RedisConnectionFactory factory) {
        SaTokenDaoForRedisTemplate dao = new SaTokenDaoForRedisTemplate();
        dao.init(factory);
        return dao;
    }



}
