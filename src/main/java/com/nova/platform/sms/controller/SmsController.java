package com.nova.platform.sms.controller;

import com.nova.common.enums.SmsTypeEnum;
import com.nova.common.result.Result;
import com.nova.platform.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信平台接口
 *
 * <p>对外提供登录/注册/换绑三种短信验证码下发能力。
 * 同一手机号 60 秒内只能请求一次（{@code RedisConstant.SmsLimit.PHONE}）。</p>
 *
 * @author huangpw
 * @date 2026/9/9
 */
@Tag(name = "08.短信平台")
@RestController
@RequestMapping("/v1/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsController {

    private final SmsService smsService;

    @Operation(summary = "发送登录短信验证码")
    @GetMapping("/login-code")
    public Result<?> loginCode(
            @Parameter(description = "手机号", required = true) @RequestParam String phone) {
        return smsService.sendCode(phone, SmsTypeEnum.LOGIN);
    }

    @Operation(summary = "发送注册短信验证码")
    @GetMapping("/register-code")
    public Result<?> registerCode(
            @Parameter(description = "手机号", required = true) @RequestParam String phone) {
        return smsService.sendCode(phone, SmsTypeEnum.REGISTER);
    }

    @Operation(summary = "发送换绑手机号验证码")
    @GetMapping("/mobile-code")
    public Result<?> mobileCode(
            @Parameter(description = "手机号", required = true) @RequestParam String phone) {
        return smsService.sendCode(phone, SmsTypeEnum.MOBILE);
    }
}
