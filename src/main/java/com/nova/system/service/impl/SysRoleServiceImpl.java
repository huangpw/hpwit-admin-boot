package com.nova.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nova.common.base.Option;
import com.nova.common.constant.SysConstant;
import com.nova.common.enums.DataScopeEnum;
import com.nova.common.utils.RoleUtil;
import com.nova.core.exception.BusinessException;
import com.nova.system.converter.SysRoleConverter;
import com.nova.system.mapper.SysRoleMapper;
import com.nova.system.model.entity.SysRole;
import com.nova.system.model.entity.SysRoleMenu;
import com.nova.system.model.form.SysRoleForm;
import com.nova.system.model.query.SysRolePageQuery;
import com.nova.system.model.vo.SysRolePageVo;
import com.nova.system.service.SysRoleMenuService;
import com.nova.system.service.SysRoleService;
import com.nova.system.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 角色业务实现类
 *
 * @author huangpw
 * @since 2026/01/03
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysRoleConverter sysRoleConverter;

    /**
     * 角色分页列表
     *
     * @param queryParams 角色查询参数
     * @return {@link Page<SysRolePageVo>} – 角色分页列表
     */
    @Override
    public Page<SysRolePageVo> getRolePage(SysRolePageQuery queryParams) {
        // 查询参数
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        String name = queryParams.getName();
        String code = queryParams.getCode();

        // 查询数据
        Page<SysRole> rolePage = this.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysRole>()
                        .like(StrUtil.isNotBlank(name), SysRole::getName, name)
                        .like(StrUtil.isNotBlank(code), SysRole::getCode, code)
                        //.and(StrUtil.isNotBlank(keywords),
                        //        wrapper ->
                        //                wrapper.like(SysRole::getName, keywords)
                        //                        .or()
                        //                        .like(SysRole::getCode, keywords)
                        //)
                        .ne(!RoleUtil.isRoot(), SysRole::getCode, SysConstant.ROOT_ROLE_CODE) // 非超级管理员不显示超级管理员角色
                        .orderByAsc(SysRole::getSort).orderByDesc(SysRole::getCreateTime).orderByDesc(SysRole::getUpdateTime)
        );

        // 实体转换
        Page<SysRolePageVo> pageVo = sysRoleConverter.toPageVo(rolePage);
        if(pageVo.getRecords() != null) {
            pageVo.getRecords().forEach(role -> {
                DataScopeEnum scopeEnum = DataScopeEnum.of(role.getDataScope());
                role.setDataScopeName(scopeEnum.getLabel());
            });
        }

        return pageVo;
    }

    /**
     * 角色下拉列表
     *
     * @return {@link List<Option>} – 角色下拉列表
     */
    @Override
    public List<Option<Long>> listRoleOptions() {
        // 查询数据
        List<SysRole> roleList = this.list(new LambdaQueryWrapper<SysRole>()
                .ne(!RoleUtil.isRoot(), SysRole::getCode, SysConstant.ROOT_ROLE_CODE)
                .select(SysRole::getId, SysRole::getName)
                .orderByAsc(SysRole::getSort)
        );

        // 实体转换
        return sysRoleConverter.toOptions(roleList);
    }

    /**
     * 保存角色
     *
     * @param roleForm 角色表单数据
     * @return {@link Boolean}
     */
    @Override
    public boolean saveRole(SysRoleForm roleForm) {

        Long roleId = roleForm.getId();

        // 编辑角色时，判断角色是否存在
        SysRole oldRole = null;
        if (roleId != null) {
            oldRole = this.getById(roleId);
            Assert.isTrue(oldRole != null, "角色不存在");
        }

        String roleCode = roleForm.getCode();
        long count = this.count(new LambdaQueryWrapper<SysRole>()
                .ne(roleId != null, SysRole::getId, roleId)
                .and(wrapper ->
                        wrapper.eq(SysRole::getCode, roleCode).or().eq(SysRole::getName, roleForm.getName())
                ));
        Assert.isTrue(count == 0, "角色名称或角色编码已存在，请修改后重试！");

        // 实体转换
        SysRole role = sysRoleConverter.toEntity(roleForm);

        boolean result = this.saveOrUpdate(role);
        if (result) {
            // 判断角色编码或状态是否修改，修改了则刷新权限缓存
            if (oldRole != null
                    && (
                    !StrUtil.equals(oldRole.getCode(), roleCode) ||
                            !ObjectUtil.equals(oldRole.getStatus(), roleForm.getStatus())
            )) {
                // TODO 刷新角色的权限缓存
                //roleMenuService.refreshRolePermsCache(oldRole.getCode(), roleCode);
            }
        }
        return result;
    }

    /**
     * 获取角色表单数据
     *
     * @param roleId 角色ID
     * @return {@link SysRoleForm} – 角色表单数据
     */
    @Override
    public SysRoleForm getRoleForm(Long roleId) {
        SysRole entity = this.getById(roleId);
        return sysRoleConverter.toForm(entity);
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态(1:启用；0:禁用)
     * @return {@link Boolean}
     */
    @Override
    public boolean updateRoleStatus(Long roleId, Integer status) {

        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        role.setStatus(status);
        boolean result = this.updateById(role);
        //if (result) {
        //    // TODO 刷新角色的权限缓存
        //    roleMenuService.refreshRolePermsCache(role.getCode());
        //}
        return result;
    }

    /**
     * 批量删除角色
     *
     * @param ids 角色ID，多个使用英文逗号(,)分割
     */
    @Override
    public void deleteRoles(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的角色ID不能为空");
        List<Long> roleIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        for (Long roleId : roleIds) {
            SysRole role = this.getById(roleId);
            Assert.isTrue(role != null, "角色不存在");

            // 判断角色是否被用户关联
            boolean isRoleAssigned = sysUserRoleService.hasAssignedUsers(roleId);
            Assert.isTrue(!isRoleAssigned, "角色【{}】已分配用户，请先解除关联后删除", role.getName());

            boolean deleteResult = this.removeById(roleId);
            if (deleteResult) {
                // TODO 删除成功，刷新权限缓存
                //roleMenuService.refreshRolePermsCache(role.getCode());
            }
        }
    }

    /**
     * 获取角色的菜单ID集合
     *
     * @param roleId 角色ID
     * @return 菜单ID集合(包括按钮权限ID)
     */
    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return sysRoleMenuService.listMenuIdsByRoleId(roleId);
    }

    /**
     * 修改角色的资源权限
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID集合
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", key = "'routes'")
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        // 删除角色菜单
        sysRoleMenuService.remove(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
        );
        // 新增角色菜单
        if (CollectionUtil.isNotEmpty(menuIds)) {
            List<SysRoleMenu> roleMenus = menuIds
                    .stream()
                    .map(menuId -> new SysRoleMenu(roleId, menuId))
                    .toList();
            sysRoleMenuService.saveBatch(roleMenus);
        }

        // TODO 刷新角色的权限缓存
        //roleMenuService.refreshRolePermsCache(role.getCode());
    }

    /**
     * 获取最大范围的数据权限
     *
     * @param roles 角色编码集合
     * @return {@link Integer} – 数据权限范围
     */
    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        Integer dataScope = this.baseMapper.getMaximumDataScope(roles);
        return dataScope;
    }

}
