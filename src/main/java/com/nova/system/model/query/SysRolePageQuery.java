package com.nova.system.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nova.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色分页查询对象
 *
 * @author Ray
 * @since 2022/6/3
 */
@Schema(description = "角色分页查询对象")
@Getter
@Setter
public class SysRolePageQuery extends BasePageQuery {

    @Schema(description="角色名称")
    private String name;

    @Schema(description="角色编码")
    private String code;

    @Schema(description="开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;

    @Schema(description="结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;
}
