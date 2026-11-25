package com.hpwit.common.result;

import lombok.Getter;

/**
 * 结果状态码枚举（对应HTTP错误码）
 * @Author huangpw
 * @Date 2026/1/5 16:48
 */
@Getter
public enum ResultCode {

    // 2xx信息性状态码
    SUCCESS(200, "请求成功"),

    // 默认错误
    //DEFAULT(-1, "请求错误"),
    //// 4xx客户端错误
    BAD_REQUEST(400, "用户请求参数错误"), // 用户请求参数错误
    ACCESS_UNAUTHORIZED(401, "用户访问未授权"),
    //FORBIDDEN(403, "服务器拒绝访问"),
    //NOT_FOUND(404, "请求的资源不存在"),
    //METHOD_NOT_ALLOWED(405, "请求方法未允许"),
    INVALID_USER_INPUT(406, "用户请求参数校验异常"),
    INTERFACE_NOT_EXIST(407, "用户请求接口不存在"),
    //REQUEST_TIMEOUT(408, "网络请求超时"),
    //
    REQUEST_REQUIRED_PARAMETER_IS_EMPTY(410, "请求必填参数为空"),
    DATABASE_ACCESS_DENIED(411, "SQL语法错误异常"),
    DATABASE_EXECUTION_SYNTAX_ERROR(412, "数据库执行语法错误"),
    PARAMETER_FORMAT_MISMATCH(421, "参数格式不匹配"),
    INTEGRITY_CONSTRAINT_VIOLATION(422, "违反了完整性约束"),
    REQUEST_CONCURRENCY_LIMIT_EXCEEDED(423, "请求并发数超出限制"), // IP 限流阈值超出
    DUPLICATE_SUBMISSION(425, "请勿重复提交"), // 防重复提交占位未释放
    //
    //USER_PASSWORD_ERROR(431, "用户名或密码错误"),
    //USER_LOGIN_EXCEPTION(432, "用户登录异常"),
    ACCESS_TOKEN_INVALID(433, "访问令牌无效或已过期"),
    //REFRESH_TOKEN_INVALID(434, "刷新令牌无效或已过期"),
    //
    // 5xx服务端错误
    INTERNAL_SERVER_ERROR(500, "用户请求服务异常"),
    //NOT_IMPLEMENTED(501, "服务器未实现请求功能"),
    //BAD_GATEWAY(502, "错误网关"),
    //SERVICE_UNAVAILABLE(503, "服务不可用"),
    //GATEWAY_TIMEOUT(504, "网关超时"),
    HTTP_VERSION_NOT_SUPPORTED(505, "http版本不支持该请求");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // 可添加根据code获取枚举的静态方法
    public static ResultCode getByCode(int code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode() == code) {
                return resultCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
