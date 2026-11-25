package com.hpwit.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 字典表单对象
 * @Author huangpw
 * @Date 2025/11/25 15:50
 */
@Data
public class SysDictForm {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "字典编码")
    @NotBlank(message = "分类编码不能为空")
    private String dictCode;

    @Schema(description = "字典名称")
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "状态（1：启用, 0：停用）")
    @Range(min = 0, max = 1, message = "字典状态不正确")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
