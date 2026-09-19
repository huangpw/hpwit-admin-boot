package com.nova.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典类型视图对象
 * @Author huangpw
 * @Date 2026/1/12 21:47
 */
@Data
@Schema( description = "数据字典类型视图对象")
public class SysDictVo {
    @Schema(description = "主键 ")
    private Long id;

    @Schema(description = "类型编码")
    private String dictCode;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "状态(0:正常;1:禁用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "修改人ID")
    private Long updateBy;
}
