package com.hpwit.common.constant;

/**
 * @Author huangpw
 * @Date 2026/1/6 16:27
 */
public interface SysConstant {

    /** 根节点ID */
    Long ROOT_NODE_ID = 0L;

    /** 系统默认密码 */
    String DEFAULT_PASSWORD = "123456";

    /** 超级管理员角色编码 */
    String ROOT_ROLE_CODE = "ROOT";

    /** 系统配置 IP的QPS限流的KEY */
    String SYSTEM_CONFIG_IP_QPS_LIMIT_KEY = "IP_QPS_THRESHOLD_LIMIT";

    // ===== 通用操作提示 =====
    String SUCCESS = "操作成功";
    String FAILED = "操作失败";

    // 登录相关
    public static final String LOGIN_USER_NOT_FOUND = "登录用户不存在";
    public static final String LOGIN_PASSWORD_ERROR = "登录密码错误";
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGOUT_SUCCESS = "登出成功";
    public static final String REGISTER_SUCCESS = "注册成功";
    public static final String REGISTER_FAILED = "注册失败";

    // 注册相关
    public static final String REGISTER_USER_EXISTS = "用户名已存在";

    // 其他通用
    public static final String UNKNOWN_ERROR = "系统错误，请联系管理员";

    // ===== 通用操作提示 =====
    String OP_SUCCESS = "操作成功";
    String OP_FAIL = "操作失败";

    String SAVE_SUCCESS = "保存成功";
    String SAVE_FAIL = "保存失败";

    String UPDATE_SUCCESS = "更新成功";
    String UPDATE_FAIL = "更新失败";

    String DELETE_SUCCESS = "删除成功";
    String DELETE_FAIL = "删除失败";

    String QUERY_SUCCESS = "查询成功";
    String QUERY_FAIL = "查询失败";

    // ===== 自定义业务提示 =====
    String USER_EXISTS_IN_UNIT = "同一单位该用户已存在";
    String PHONE_EXISTS = "联系电话已存在";
    String EPC_EXISTS = "EPC已存在";
    String TCP_PORT_EXISTS = "TCP端口已存在";
    String TCP_PORT_NOT_NULL = "TCP端口不能为空";
}
