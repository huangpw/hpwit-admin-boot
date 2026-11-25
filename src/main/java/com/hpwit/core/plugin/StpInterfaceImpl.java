package com.hpwit.core.plugin;

import cn.dev33.satoken.stp.StpInterface;
import com.hpwit.system.mapper.SysMenuMapper;
import com.hpwit.system.mapper.SysRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 自定义权限加载接口实现类
 *
 * @Author huangpw
 * @Date 2026/1/6 19:11
 */
@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysMenuMapper sysMenuMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /**
     * 获取用户权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        // 获取用户ID
        Long userId = Long.parseLong(loginId.toString());
        return sysMenuMapper.selectPermListByUserId(userId);
    }

    /**
     * 获取用户角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        Long userId = Long.parseLong(loginId.toString());
        return sysRoleMapper.selectRoleCodeListByUserId(userId);
    }
}
