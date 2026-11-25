package com.hpwit.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author huangpw
 * @Date 2026/1/16 9:21
 */
@Data
public class SysUserOnlineVo {

    /** 用户ID */
    private Object loginId;

    /** 会话编号 */
    private String token;

    /** 登录名称 */
    private String username;

    /** 所属部门 */
    private String deptName;

    /** 主机 */
    private String ip;

    /** 登录地点 */
    private String loginLocation;

    /** 操作系统 */
    private String os;

    /** 浏览器 */
    private String browser;

    /** 登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
