package com.hpwit.common.result;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构体
 * @Author huangpw
 * @Date 2026/1/5 16:49
 */
@Data
public class Result<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> failed() {
        return result(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), null);
    }

    public static <T> Result<T> failed(String msg) {
        return result(ResultCode.BAD_REQUEST.getCode(), msg, null);
    }

    public static <T> Result<T> judge(boolean status) {
        if (status) {
            return success();
        } else {
            return failed();
        }
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        return result(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> failed(ResultCode resultCode, String message) {
        return result(resultCode.getCode(), StrUtil.isNotBlank(message) ? message : resultCode.getMessage(), null);
    }

    private static <T> Result<T> result(ResultCode resultCode, T data) {
        return result(resultCode.getCode(), resultCode.getMessage(), data);
    }

    private static <T> Result<T> result(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setData(data);
        result.setMessage(message);
        return result;
    }

    public static boolean isSuccess(Result<?> result) {
        return result != null && Integer.valueOf(ResultCode.SUCCESS.getCode()).equals(result.getCode());
    }
}
