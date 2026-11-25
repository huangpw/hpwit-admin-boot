package com.hpwit.system.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hpwit.common.base.Option;
import com.hpwit.system.model.entity.SysRole;
import com.hpwit.system.model.form.SysRoleForm;
import com.hpwit.system.model.query.SysRolePageQuery;
import com.hpwit.system.model.vo.SysRolePageVo;

import java.util.List;
import java.util.Set;

/**
 * 角色业务接口层
 *
 * @author haoxr
 * @since 2022/6/3
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 角色分页列表
     * @param query
     * @return
     */
    Page<SysRolePageVo> getRolePage(SysRolePageQuery query);


    /**
     * 角色下拉列表
     *
     * @return
     */
    List<Option<Long>> listRoleOptions();

    /**
     *
     * @param roleForm
     * @return
     */
    boolean saveRole(SysRoleForm roleForm);

    /**
     * 获取角色表单数据
     *
     * @param roleId 角色ID
     * @return  {@link SysRoleForm} – 角色表单数据
     */
    SysRoleForm getRoleForm(Long roleId);

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态(1:启用；0:禁用)
     * @return {@link Boolean}
     */
    boolean updateRoleStatus(Long roleId, Integer status);

    /**
     * 批量删除角色
     *
     * @param ids 角色ID，多个使用英文逗号(,)分割
     */
    void deleteRoles(String ids);

    /**
     * 获取角色的菜单ID集合
     *
     * @param roleId 角色ID
     * @return 菜单ID集合(包括按钮权限ID)
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 修改角色的资源权限
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID集合
     */
    void assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 获取最大范围的数据权限
     * @param roles
     * @return
     */
    Integer getMaximumDataScope(Set<String> roles);


}
