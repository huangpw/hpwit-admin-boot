package com.nova.system.model.query;

import com.nova.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author huangpw
 * @Date 2026/1/16 9:26
 */
@Schema(description = "在线分页查询对象")
@Getter
@Setter
public class SysUserOnlinePageQuery extends BasePageQuery {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录地址")
    private String loginLocation;
}
