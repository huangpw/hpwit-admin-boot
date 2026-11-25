package com.hpwit.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hpwit.system.mapper.SysUserRoleMapper;
import com.hpwit.system.model.entity.SysUserRole;
import com.hpwit.system.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.authentication.core.TokenManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author AlbertHPW
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {


  //@Autowired
  //private TokenManager tokenManager;

  /**
   * 保存用户角色
   *
   * @param userId 用户ID
   * @param roleIds 选择的角色ID集合
   * @return
   */
  @Override
  public void saveUserRoles(Long userId, List<Long> roleIds) {
    if (userId == null || CollectionUtil.isEmpty(roleIds)) {
      return ;
    }

    // 获取现有角色
    List<Long> userRoleIds = this.list(new LambdaQueryWrapper<SysUserRole>()
        .select(SysUserRole::getRoleId)
        .eq(SysUserRole::getUserId, userId))
      .parallelStream()
      .map(SysUserRole::getRoleId)
      .toList();

    // 使用Set提升对比效率
    Set<Long> oldRoles = new HashSet<>(userRoleIds);
    Set<Long> newRoles = new HashSet<>(roleIds);

    // 计算变更集
    Set<Long> addedRoles = new HashSet<>(newRoles);
    addedRoles.removeAll(oldRoles);

    Set<Long> removedRoles = new HashSet<>(oldRoles);
    removedRoles.removeAll(newRoles);

    boolean rolesChanged = !addedRoles.isEmpty() || !removedRoles.isEmpty();

    // 批量保存新增角色
    if (!addedRoles.isEmpty()) {
      this.saveBatch(addedRoles.stream()
        .map(roleId -> new SysUserRole(userId, roleId))
        .collect(Collectors.toList()));
    }

    // 删除废弃角色
    if (!removedRoles.isEmpty()) {
      this.remove(new LambdaQueryWrapper<SysUserRole>()
        .eq(SysUserRole::getUserId, userId)
        .in(SysUserRole::getRoleId, removedRoles));
    }

    // 当权限变更时清除登录态
    if (rolesChanged) {
      // 获取用户所有有效token（根据实际token存储实现）
      String accessToken = String.valueOf(StpUtil.getTokenInfo());
      //tokenManager.invalidateToken(accessToken);
    }
  }

  /**
   * 判断角色是否存在绑定的用户
   *
   * @param roleId 角色ID
   * @return true：已分配 false：未分配
   */
  @Override
  public boolean hasAssignedUsers(Long roleId) {
    int count = this.baseMapper.countUsersForRole(roleId);
    return count > 0;
  }
}
