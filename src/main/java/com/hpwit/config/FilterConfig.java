package com.hpwit.config;

import com.hpwit.core.filter.RateLimiterFilter;
import com.hpwit.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Servlet 过滤器注册
 *
 * <p>youlai-boot 中 IP 限流过滤器注册在 Spring Security 过滤器链
 * （UsernamePasswordAuthenticationFilter 之前），nova 使用 Sa-Token（MVC 拦截器），
 * 故此处通过 {@link FilterRegistrationBean} 统一注册。</p>
 *
 * @author huangpw
 * @date 2026/9/18
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final RedisTemplate<String, Object> redisTemplate;

    private final SysConfigService sysConfigService;

    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilter() {
        FilterRegistrationBean<RateLimiterFilter> registration =
                new FilterRegistrationBean<>(new RateLimiterFilter(redisTemplate, sysConfigService));
        registration.addUrlPatterns("/*");
        // 排在 CORS 过滤器（order=-101）之后，避免预检请求被计数限流；
        // 早于 Sa-Token 拦截器等 MVC 层逻辑执行
        registration.setOrder(-100);
        registration.setName("rateLimiterFilter");
        return registration;
    }
}
