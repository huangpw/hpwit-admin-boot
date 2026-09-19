package com.nova.system.model.query;

import cn.hutool.db.sql.Direction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nova.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户分页查询列表
 * @Author huangpw
 * @Date 2025/11/25 16:06
 */
@Data
@Schema
@EqualsAndHashCode(callSuper = false) // 不调用父类的equals和hashCode方法
public class SysUserPageQuery extends BasePageQuery {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description="手机号")
    private String mobile;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "创建时间范围")
    private List<String> createTime;

    @Schema(description = "排序的字段")
    //@ValidField(allowedValues = {"create_time", "update_time"})
    private String field;

    @Schema(description = "排序方式（正序:ASC；反序:DESC）")
    private Direction direction;

    /**
     * 是否超级管理员
     */
    @JsonIgnore
    @Schema(hidden = true)
    private Boolean isRoot;

}
