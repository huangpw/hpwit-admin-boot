package com.hpwit.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录返回实体对象
 * @Author huangpw
 * @Date 2026/1/6 16:16
 */
@Schema(description ="登录返回实体对象")
@Data
public class LoginVo {

    @Schema(description="用户ID")
    private Long id;

    @Schema(description="用户名")
    private String userName;

    @Schema(description="用户昵称")
    private String nickname;

    @Schema(description="accessToken")
    private String accessToken;

    @Schema(description="refreshToken")
    private String refreshToken;

    @Schema(description="头像")
    private String avatar;

    @Schema(description="角色集合")
    private List<String> roles;

    @Schema(description="权限集合")
    private List<String> perms;
}
