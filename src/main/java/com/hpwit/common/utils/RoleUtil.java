package com.hpwit.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.hpwit.common.constant.SysConstant;

import java.util.List;

/**
 * @Author huangpw
 * @Date 2026/1/7 10:59
 */
public class RoleUtil {

    /**
     * 是否超级管理员
     * <p>
     * 超级管理员忽视任何权限判断
     */
    public static boolean isRoot() {
        List<String> roleList = StpUtil.getRoleList();
        if (CollectionUtil.isEmpty(roleList)) {
            return false;
        }
        return roleList.stream().anyMatch(role -> role.equalsIgnoreCase(SysConstant.ROOT_ROLE_CODE));

    }
}
