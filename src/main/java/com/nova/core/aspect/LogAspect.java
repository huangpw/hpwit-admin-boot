package com.nova.core.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONUtil;
import com.nova.common.enums.LogModuleEnum;
import com.nova.common.utils.IPUtil;
import com.nova.system.model.entity.SysLog;
import com.nova.system.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 日志切面
 *
 * @author Ray.Hao
 * @since 2024/6/25
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {
    // 日志业务层接口
    private final SysLogService sysLogService;
    private final HttpServletRequest request;
    private final CacheManager cacheManager;


    /**
     * 切点
     */
    @Pointcut("@annotation(com.nova.core.annotation.Log)")
    public void logPointcut() {
    }

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @Around("logPointcut() && @annotation(logAnnotation)")
    public Object doAround(ProceedingJoinPoint joinPoint, com.nova.core.annotation.Log logAnnotation) throws Throwable {
        TimeInterval timer = DateUtil.timer();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = timer.interval(); // 执行时长
            // proceed 后再判断登录状态：登录接口执行完 SysUserServiceImpl.login() 内部 StpUtil.login(user.getId()) 已签发 token，
            // 此时 StpUtil.isLogin() 为 true；常规接口则前后均为 true。登录失败/异常场景下仍为 null，属预期。
            Long userId = null;
            if (StpUtil.isLogin()) {
                userId = Long.valueOf(StpUtil.getLoginId().toString());
            }
            this.saveLog(joinPoint, exception, result, logAnnotation, executionTime, userId);
        }
        return result;
    }


    /**
     * 保存日志
     *
     * @param joinPoint     切点
     * @param e             异常
     * @param jsonResult    响应结果
     * @param logAnnotation 日志注解
     * @param userId        用户ID
     */
    private void saveLog(final JoinPoint joinPoint, final Exception e, Object jsonResult,
                         com.nova.core.annotation.Log logAnnotation, long executionTime, Long userId) {
        String requestURI = request.getRequestURI();

        log.error("logAnnotation: {}", logAnnotation);
        // 创建日志记录
        SysLog log = new SysLog();
        log.setExecutionTime(executionTime);
        if (logAnnotation == null && e != null) {
            log.setModule(LogModuleEnum.EXCEPTION);
            log.setContent("系统发生异常");
            this.setRequestParameters(joinPoint, log);
            log.setResponseContent(JSONUtil.toJsonStr(e.getStackTrace()));
        } else {
            log.setModule(logAnnotation.module());
            log.setContent(logAnnotation.value());
            // 请求参数
            if (logAnnotation.params()) {
                this.setRequestParameters(joinPoint, log);
            }
            // 响应结果
            if (logAnnotation.result() && jsonResult != null) {
                log.setResponseContent(JSONUtil.toJsonStr(jsonResult));
            }
        }
        log.setRequestUri(requestURI);
        log.setCreateBy(userId);
        String ipAddr = IPUtil.getIpAddr(request);
        if (StrUtil.isNotBlank(ipAddr)) {
            log.setIp(ipAddr);
            String region = IPUtil.getRegion(ipAddr);
            // 中国|0|四川省|成都市|电信 解析省和市
            if (StrUtil.isNotBlank(region)) {
                String[] regionArray = region.split("\\|");
                if (regionArray.length > 2) {
                    log.setProvince(regionArray[2]);
                    log.setCity(regionArray[3]);
                }
            }
        }


        // 获取浏览器和终端系统信息
        String userAgentString = request.getHeader("User-Agent");
        UserAgent userAgent = resolveUserAgent(userAgentString);
        if (Objects.nonNull(userAgent)) {
            // 系统信息
            log.setOs(userAgent.getOs().getName());
            // 浏览器信息
            log.setBrowser(userAgent.getBrowser().getName());
            log.setBrowserVersion(userAgent.getBrowser().getVersion(userAgentString));
        }
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        log.setMethod(methodName);
        // 保存日志到数据库
        sysLogService.save(log);
    }

    /**
     * 设置请求参数到日志对象中
     *
     * @param joinPoint 切点
     * @param log       操作日志
     */
    private void setRequestParameters(JoinPoint joinPoint, SysLog log) {
        String requestMethod = request.getMethod();
        log.setRequestMethod(requestMethod);
        if (HttpMethod.GET.name().equalsIgnoreCase(requestMethod) || HttpMethod.PUT.name().equalsIgnoreCase(requestMethod) || HttpMethod.POST.name().equalsIgnoreCase(requestMethod)) {
            String params = convertArgumentsToString(joinPoint.getArgs());
            log.setRequestParams(StrUtil.sub(params, 0, 65535));
        } else {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Map<?, ?> paramsMap =
                        (Map<?, ?>) attributes.getRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                log.setRequestParams(StrUtil.sub(paramsMap.toString(), 0, 65535));
            } else {
                log.setRequestParams("");
            }
        }
    }

    /**
     * 将参数数组转换为字符串
     *
     * @param paramsArray 参数数组
     * @return 参数字符串
     */
    private String convertArgumentsToString(Object[] paramsArray) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null) {
            for (Object param : paramsArray) {
                if (!shouldFilterObject(param)) {
                    // 如果是基本类型或者枚举类型，直接添加到参数字符串中
                    if (param.getClass().isPrimitive() || param.getClass().isEnum()) {
                        params.append(param).append(" ");
                    } else {
                        params.append(JSONUtil.toJsonStr(param)).append(" ");
                    }
                }
            }
        }
        return params.toString().trim();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param obj 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    private boolean shouldFilterObject(Object obj) {
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return MultipartFile.class.isAssignableFrom(clazz.getComponentType());
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) obj;
            return collection.stream().anyMatch(item -> item instanceof MultipartFile);
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) obj;
            return map.values().stream().anyMatch(value -> value instanceof MultipartFile);
        }
        return obj instanceof MultipartFile || obj instanceof HttpServletRequest || obj instanceof HttpServletResponse;
    }


    /**
     * 解析UserAgent
     *
     * @param userAgentString UserAgent字符串
     * @return UserAgent
     */
    public UserAgent resolveUserAgent(String userAgentString) {
        if (StrUtil.isBlank(userAgentString)) {
            return null;
        }
        // 给userAgentStringMD5加密一次防止过长
        String userAgentStringMD5 = DigestUtil.md5Hex(userAgentString);
        //判断是否命中缓存
        UserAgent userAgent = Objects.requireNonNull(cacheManager.getCache("userAgent")).get(userAgentStringMD5,
                UserAgent.class);
        if (userAgent != null) {
            return userAgent;
        }
        userAgent = UserAgentUtil.parse(userAgentString);
        Objects.requireNonNull(cacheManager.getCache("userAgent")).put(userAgentStringMD5, userAgent);
        return userAgent;
    }

}
