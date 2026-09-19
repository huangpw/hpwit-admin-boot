package com.nova.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nova.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * @Author huangpw
 * @Date 2025/11/17 17:09
 */
@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = false) // 不继承父类属性
public class SysUser extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 性别(1-男 2-女 0-保密) */
    private Integer gender;

    /** 密码 */
    private String password;

    /** 加密盐 */
    private String salt;

    /** 部门ID */
    private Long deptId;

    /** 用户头像 */
    private String avatar;

    /** 联系方式 */
    private String mobile;

    /** 状态(1-正常 0-禁用) */
    private Integer status;

    /** 用户邮箱 */
    private String email;

    /** 微信 openid */
    private String openid;

    /** 逻辑删除标识(0-未删除 1-已删除) */
    @TableLogic
    private Integer isDeleted;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 修改人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

}
