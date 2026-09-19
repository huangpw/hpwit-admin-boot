package com.nova.platform.mail.controller;

import com.nova.common.enums.EmailTypeEnum;
import com.nova.common.result.Result;
import com.nova.platform.mail.model.dto.MailDto;
import com.nova.platform.mail.service.MailCodeService;
import com.nova.platform.mail.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * 邮件平台接口
 *
 * <p>对外提供两类能力：</p>
 * <ul>
 *   <li>通用邮件下发（{@code POST /v1/mail/send} 等）—— 调用底层 {@link MailService}</li>
 *   <li>邮箱验证码（{@code GET /v1/mail/{login,register,reset-password,bind}-code}）—— 调用 {@link MailCodeService}</li>
 * </ul>
 *
 * @author huangpw
 * @date 2026/2/26
 */
@Tag(name = "09.邮件平台")
@RestController
@RequestMapping("/v1/mail")
@RequiredArgsConstructor
@Slf4j
public class MailController {

    private final MailService mailService;
    private final MailCodeService mailCodeService;

    // ==================== 通用邮件下发 ====================

    @Operation(summary = "发送简单文本邮件")
    @PostMapping("/send")
    public Result<?> sendMail(@RequestBody MailDto mailDto) {
        mailService.sendMail(mailDto.getTo(), mailDto.getSubject(), mailDto.getContent());
        return Result.success("邮件发送成功");
    }

    @Operation(summary = "发送带附件的邮件")
    @GetMapping("/send/attachment")
    public Result<?> sendMailAttachment(MailDto mailDto) {
        // 建议增加简单的路径校验
        File file = new File(mailDto.getFilePath());
        if (!file.exists()) {
            return Result.failed("发送失败：文件不存在");
        }
        mailService.sendMailWithAttachment(mailDto.getTo(), mailDto.getSubject(), mailDto.getContent(), mailDto.getFilePath());
        return Result.success("邮件发送成功");
    }

    // ==================== 邮箱验证码 ====================
    // 之前在 AuthController，现已迁移到 platform/mail 模块

    @Operation(summary = "发送登录邮箱验证码")
    @GetMapping("/login-code")
    public Result<?> loginCode(
            @Parameter(description = "邮箱地址", required = true) @RequestParam String email) {
        return mailCodeService.sendCode(email, EmailTypeEnum.LOGIN);
    }

    @Operation(summary = "发送注册邮箱验证码")
    @GetMapping("/register-code")
    public Result<?> registerCode(
            @Parameter(description = "邮箱地址", required = true) @RequestParam String email) {
        return mailCodeService.sendCode(email, EmailTypeEnum.REGISTER);
    }

    @Operation(summary = "发送找回密码邮箱验证码")
    @GetMapping("/reset-password-code")
    public Result<?> resetPasswordCode(
            @Parameter(description = "邮箱地址", required = true) @RequestParam String email) {
        return mailCodeService.sendCode(email, EmailTypeEnum.RESET_PASSWORD);
    }

    @Operation(summary = "发送换绑邮箱验证码")
    @GetMapping("/bind-code")
    public Result<?> bindCode(
            @Parameter(description = "邮箱地址", required = true) @RequestParam String email) {
        return mailCodeService.sendCode(email, EmailTypeEnum.BIND);
    }
}
