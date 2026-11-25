package com.hpwit.core.exception;

import com.hpwit.common.result.ResultCode;
import lombok.Getter;
import org.slf4j.helpers.MessageFormatter;

/**
 * 自定义业务异常
 *
 * @author Ray
 * @since 2022/7/31
 */
@Getter
public class BusinessException extends RuntimeException {

    public ResultCode resultCode;

    public BusinessException(ResultCode errorCode) {
        super(errorCode.getMessage());
        this.resultCode = errorCode;
    }


    public BusinessException(ResultCode errorCode,String message) {
        super(message);
        this.resultCode = errorCode;
    }


    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message, Object... args) {
        super(formatMessage(message, args));
    }

    private static String formatMessage(String message, Object... args) {
        return MessageFormatter.arrayFormat(message, args).getMessage();
    }
}
