package com.hpwit.system.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户列表导出
 * @Author huangpw
 * @Date 2026/1/9 14:49
 */
@Data
public class SysUserExportVo {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("性别")
    private String gender; // 男 / 女 / 保密

    @ExcelProperty("部门ID")
    private Integer deptId;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("头像")
    private String avatar;

    @ExcelProperty("备注")
    private String remark;
}
