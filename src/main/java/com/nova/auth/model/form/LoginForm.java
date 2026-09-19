package com.nova.auth.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录表单
 * @Author: AlbertHPW
 * @Date: 2025/11/22 11:27
 */
@Data
@Schema
public class LoginForm {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "验证码秘钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaKey;

    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaCode;
}
