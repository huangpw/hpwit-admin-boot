package com.nova.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应
 *
 * <p>前端通过 {@code <img :src="data.img">} 直接展示，返回的 {@code key} 在登录时连同用户输入的验证码一起提交。</p>
 *
 * @author huangpw
 * @date 2026/9/9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图形验证码响应")
public class CaptchaVo {

    @Schema(description = "验证码 key（登录时回传）")
    private String key;

    @Schema(description = "验证码 base64 图片（data:image/png;base64,xxx）")
    private String img;
}
