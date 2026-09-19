package com.nova.core.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.nova.common.constant.RedisConstant;
import com.nova.common.result.ResultCode;
import com.nova.common.utils.IPUtil;
import com.nova.core.annotation.RepeatSubmit;
import com.nova.core.exception.BusinessException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 防重复提交切面
 *
 * <p>用 Redis 的「SETNX + TTL」原子占位实现：同一请求标识在过期时间内
 * 第二次提交直接拒绝，占位到期自动释放，无需手动解锁。</p>
 *
 * <p>移植自 youlai-boot（{@code com.youlai.boot.framework.web.repeatsubmit.RepeatSubmitAspect}，
 * by Ray.Hao @since 4.3.1），由 Redisson 替换为 nova 现成的 {@link StringRedisTemplate}。</p>
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final StringRedisTemplate redisTemplate;

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /** 生成请求指纹时排除的参数类型，这些对象序列化结果不稳定或无业务含义 */
    private static final List<Class<?>> EXCLUDED_PARAM_TYPES =
            List.of(ServletRequest.class, ServletResponse.class, MultipartFile.class, BindingResult.class);

    /** Authorization 头 Bearer 前缀 */
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    @Pointcut("@annotation(repeatSubmit)")
    public void repeatSubmitPointCut(RepeatSubmit repeatSubmit) {
    }

    @Around(value = "repeatSubmitPointCut(repeatSubmit)", argNames = "pjp,repeatSubmit")
    public Object handleRepeatSubmit(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String lockKey = buildLockKey(pjp, repeatSubmit);
        // SETNX + EX：原子操作，key 不存在才写入并设置 TTL
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(repeatSubmit.expire()));
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMISSION);
        }
        return pjp.proceed();
    }

    /**
     * 生成防重占位的 key
     *
     * <p>注解配置了 key 表达式时以表达式的值区分请求；否则对过滤后的业务参数做哈希。</p>
     */
    private String buildLockKey(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String userIdentifier = getUserIdentifier(request);
        String requestIdentifier = StrUtil.join(":", request.getMethod(), request.getRequestURI());
        String requestFingerprint = StrUtil.isNotBlank(repeatSubmit.key())
                ? DigestUtil.sha256Hex(evaluateKey(pjp, repeatSubmit.key()))
                : DigestUtil.sha256Hex(JSONUtil.toJsonStr(filterArgs(pjp)));
        return StrUtil.format(RedisConstant.Lock.RESUBMIT, userIdentifier,
                requestIdentifier + ":" + requestFingerprint);
    }

    /** 过滤掉不可稳定序列化的参数后再生成指纹 */
    private Object[] filterArgs(ProceedingJoinPoint pjp) {
        return Arrays.stream(pjp.getArgs())
                .filter(arg -> arg == null || EXCLUDED_PARAM_TYPES.stream().noneMatch(type -> type.isInstance(arg)))
                .toArray();
    }

    /** 解析注解上的 SpEL key 表达式，方法参数可直接引用 */
    private String evaluateKey(ProceedingJoinPoint pjp, String spel) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                pjp.getTarget(), method, pjp.getArgs(), PARAMETER_NAME_DISCOVERER);
        Object value = SPEL_PARSER.parseExpression(spel).getValue(context);
        return value != null ? value.toString() : "";
    }

    /**
     * 获取用户唯一标识
     * 1. 从请求头中获取 Token，使用 SHA-256 加密 Token 作为用户唯一标识
     * 2. 如果 Token 为空，使用 IP 作为用户唯一标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String tokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(tokenHeader) && tokenHeader.startsWith(BEARER_TOKEN_PREFIX)) {
            String rawToken = tokenHeader.substring(BEARER_TOKEN_PREFIX.length());
            return DigestUtil.sha256Hex(rawToken);
        }
        return IPUtil.getIpAddr(request);
    }
}