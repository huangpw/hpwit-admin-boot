package com.hpwit.common.utils;

import cn.hutool.json.JSONUtil;
import com.hpwit.common.result.Result;
import com.hpwit.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Web 响应写出工具
 *
 * <p>用于过滤器等无法走 {@code @RestControllerAdvice} 统一异常处理的场景，
 * 直接向响应体写出 {@link Result} JSON。</p>
 *
 * <p>移植自 youlai-boot（{@code com.youlai.boot.core.web.WebResponseWriter}），
 * 精简为仅保留限流过滤器所需的错误写出。</p>
 *
 * @author huangpw
 * @date 2026/9/18
 */
@Slf4j
public final class WebUtil {

    private WebUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 写出错误响应（JSON），HTTP 状态码与业务码保持一致
     *
     * @param response   HttpServletResponse
     * @param resultCode 业务结果码
     */
    public static void writeError(HttpServletResponse response, ResultCode resultCode) {
        try {
            response.setStatus(resultCode.getCode());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write(JSONUtil.toJsonStr(Result.failed(resultCode)));
        } catch (IOException e) {
            log.error("写入响应时发生未知异常: resultCode={}", resultCode, e);
        }
    }
}
