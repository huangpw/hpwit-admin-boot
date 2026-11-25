package com.hpwit.system.controller;

import com.hpwit.common.base.Option;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.dto.CurrentMenuDto;
import com.hpwit.system.model.form.SysMenuForm;
import com.hpwit.system.model.query.SysMenuQuery;
import com.hpwit.system.model.vo.SysMenuVo;
import com.hpwit.system.model.vo.SysRouteVo;
import com.hpwit.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单控制层
 *
 * @author Ray.Hao
 * @since 2020/11/06
 */
@Tag(name = "04.菜单接口")
@RestController
@RequestMapping("/v1/menu")
@Slf4j
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @Operation(summary = "菜单列表")
    @GetMapping("/list")
    //@Log(value = "菜单列表", module = LogModuleEnum.MENU)
    public Result<List<SysMenuVo>> getMenus(SysMenuQuery query) {
        List<SysMenuVo> menuList = sysMenuService.listMenus(query);
        return Result.success(menuList);
    }

    @Operation(summary = "菜单下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> getMenuOptions(@Parameter(description = "是否只查询父级菜单") @RequestParam(required = false, defaultValue = "false") boolean onlyParent) {
        List<Option<Long>> menus = sysMenuService.listMenuOptions(onlyParent);
        return Result.success(menus);
    }

    @Operation(summary = "当前用户菜单路由列表")
    @GetMapping("/routes")
    public Result<List<SysRouteVo>> getCurrentUserRoutes() {
        List<SysRouteVo> routeList = sysMenuService.listCurrentUserRoutes();
        return Result.success(routeList);
    }

    @Operation(summary = "当前用户菜单路由列表")
    @GetMapping("/routes/current")
    public Result<List<CurrentMenuDto>> getCurrentUserMenuRoutes() {
        List<CurrentMenuDto> routeList = sysMenuService.listCurrentUserMenuRoutes();
        return Result.success(routeList);
    }

    @Operation(summary = "菜单表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    public Result<SysMenuForm> getMenuForm(@Parameter(description = "菜单ID") @PathVariable Long id) {
        SysMenuForm menu = sysMenuService.getMenuForm(id);
        return Result.success(menu);
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('sys:menu:add')")
    //@RepeatSubmit
    public Result<?> addMenu(@RequestBody SysMenuForm menuForm) {
        boolean result = sysMenuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "修改菜单")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    public Result<?> updateMenu(@RequestBody SysMenuForm menuForm) {
        boolean result = sysMenuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:menu:delete')")
    public Result<?> deleteMenu(@Parameter(description = "菜单ID，多个以英文(,)分割") @PathVariable("id") Long id) {
        boolean result = sysMenuService.deleteMenu(id);
        return Result.judge(result);
    }

    @Operation(summary = "修改菜单显示状态")
    @PatchMapping("/{menuId}")
    //@PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    public Result<?> updateMenuVisible(@Parameter(description = "菜单ID") @PathVariable Long menuId, @Parameter(description = "显示状态(1:显示;0:隐藏)") Integer visible) {
        boolean result = sysMenuService.updateMenuVisible(menuId, visible);
        return Result.judge(result);
    }

}

