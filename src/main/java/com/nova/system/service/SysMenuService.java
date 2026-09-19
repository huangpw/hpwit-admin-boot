package com.nova.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nova.common.base.Option;
import com.nova.system.model.dto.CurrentMenuDto;
import com.nova.system.model.entity.SysMenu;
import com.nova.system.model.form.SysMenuForm;
import com.nova.system.model.query.SysMenuQuery;
import com.nova.system.model.vo.SysMenuVo;
import com.nova.system.model.vo.SysRouteVo;

import java.util.List;

/**
 * 菜单管理业务接口
 * @Author: AlbertHPW
 * @Date: 2026/1/7 0:11
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 获取菜单表格列表
     */
    List<SysMenuVo> listMenus(SysMenuQuery query);

    /**
     * 获取菜单下拉列表
     * @param onlyParent 是否只查询父级菜单
     */
    List<Option<Long>> listMenuOptions(boolean onlyParent);

    /**
     * 新增菜单
     * @param menuForm  菜单表单对象
     */
    boolean saveMenu(SysMenuForm menuForm);

    /**
     * 获取当前用户的菜单路由列表
     */
    List<SysRouteVo> listCurrentUserRoutes();

    /**
     * 获取菜单表单数据
     * @param id 菜单ID
     */
    SysMenuForm getMenuForm(Long id);

    /**
     * 删除菜单
     * @param id 菜单ID
     */
    boolean deleteMenu(Long id);

    /**
     * 修改菜单显示状态
     * @param menuId 菜单ID
     * @param visible 是否显示(1-显示 0-隐藏)
     */
    boolean updateMenuVisible(Long menuId, Integer visible);

    /**
     * 获取当前用户的菜单路由列表
     */
    List<CurrentMenuDto> listCurrentUserMenuRoutes();
}
