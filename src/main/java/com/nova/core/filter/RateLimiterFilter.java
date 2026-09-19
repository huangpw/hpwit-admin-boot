package com.nova.core.filter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.nova.common.constant.RedisConstant;
import com.nova.common.constant.SysConstant;
import com.nova.common.result.ResultCode;
import com.nova.common.utils.IPUtil;
import com.nova.common.utils.WebUtil;
import com.nova.system.service.SysConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * IP 限流过滤器
 *
 * <p>限制同一 IP 每秒最多请求次数（默认 10 次），阈值通过系统配置
 * {@code sys_config} 表的 {@link SysConstant#SYSTEM_CONFIG_IP_QPS_LIMIT_KEY} 动态调整，
 * 值缓存在 Redis Hash（{@code system:config}）中，实时生效。</p>
 *
 * <p>移植自 youlai-boot（{@code com.youlai.boot.core.filter.RateLimiterFilter}，
 * by Theo @since 2024/08/10），适配 nova 的 Sa-Token 架构：
 * youlai 注册在 Spring Security 过滤器链（UsernamePasswordAuthenticationFilter 之前），
 * nova 无 Security 过滤器链，改为 FilterRegistrationBean 注册在 CORS 过滤器之后。</p>
 *
 * @author Theo
 * @since 2024/08/10
 */
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    private final SysConfigService sysConfigService;

    /** 系统未配置限流阈值时的默认值（次/秒） */
    private static final long DEFAULT_IP_LIMIT = 10L;

    public RateLimiterFilter(RedisTemplate<String, Object> redisTemplate, SysConfigService sysConfigService) {
        this.redisTemplate = redisTemplate;
        this.sysConfigService = sysConfigService;
    }

    /**
     * 判断 IP 是否触发限流
     *
     * <p>基于 Redis INCR + 1 秒过期窗口计数；如果系统未配置限流阈值，跳过限流。</p>
     *
     * @param ip IP 地址
     * @return true 表示限流；false 表示未限流
     */
    public boolean rateLimit(String ip) {
        // 限流 Redis 键
        String key = StrUtil.format(RedisConstant.RateLimiter.IP, ip);

        // 自增请求计数
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null || count == 1) {
            // 第一次访问时设置过期时间为 1 秒
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }

        // 获取系统配置的限流阈值
        Object systemConfig = sysConfigService.getSystemConfig(SysConstant.SYSTEM_CONFIG_IP_QPS_LIMIT_KEY);
        if (systemConfig == null) {
            // 系统未配置限流，跳过限流逻辑
            log.warn("系统未配置限流阈值，跳过限流");
            return false;
        }

        // 转换系统配置为限流值，默认为 10
        long limit = Convert.toLong(systemConfig, DEFAULT_IP_LIMIT);
        return count != null && count > limit;
    }

    /**
     * 执行 IP 限流逻辑
     *
     * <p>如果 IP 请求超出限制，直接返回限流响应；否则继续执行过滤器链。</p>
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取请求的 IP 地址
        String ip = IPUtil.getIpAddr(request);

        // 判断是否限流
        if (rateLimit(ip)) {
            // 返回限流错误信息
            WebUtil.writeError(response, ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED);
            return;
        }

        // 未触发限流，继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
