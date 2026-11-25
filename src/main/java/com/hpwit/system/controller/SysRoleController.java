package com.hpwit.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hpwit.common.base.Option;
import com.hpwit.common.result.PageResult;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.form.SysRoleForm;
import com.hpwit.system.model.query.SysRolePageQuery;
import com.hpwit.system.model.vo.SysRolePageVo;
import com.hpwit.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制层
 *
 * @author Ray.Hao
 * @since 2022/10/16
 */
@Tag(name = "03.角色接口")
@RestController
@RequestMapping("/v1/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Operation(summary = "角色分页列表")
    @GetMapping("/page")
    //@Log(value = "角色分页列表", module = LogModuleEnum.ROLE)
    public PageResult<SysRolePageVo> getRolePage(SysRolePageQuery query) {
        Page<SysRolePageVo> result = sysRoleService.getRolePage(query);
        return PageResult.success(result);
    }

    @Operation(summary = "角色下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoleOptions() {
        List<Option<Long>> list = sysRoleService.listRoleOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('sys:role:add')")
    //@RepeatSubmit
    public Result<?> addRole(@Valid @RequestBody SysRoleForm roleForm) {
        boolean result = sysRoleService.saveRole(roleForm);
        return Result.judge(result);
    }

    @Operation(summary = "获取角色表单数据")
    @GetMapping("/{roleId}/form")
    //@PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<SysRoleForm> getRoleForm(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        SysRoleForm roleForm = sysRoleService.getRoleForm(roleId);
        return Result.success(roleForm);
    }

    @Operation(summary = "修改角色")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<?> updateRole(@Valid @RequestBody SysRoleForm roleForm) {
        boolean result = sysRoleService.saveRole(roleForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('sys:role:delete')")
    public Result<Void> deleteRoles(@Parameter(description = "删除角色，多个以英文逗号(,)拼接") @PathVariable String ids) {
        sysRoleService.deleteRoles(ids);
        return Result.success();
    }

    @Operation(summary = "修改角色状态")
    @PutMapping(value = "/{roleId}/status")
    //@PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<?> updateRoleStatus(@Parameter(description = "角色ID") @PathVariable Long roleId, @Parameter(description = "状态(1:启用;0:禁用)") @RequestParam Integer status) {
        boolean result = sysRoleService.updateRoleStatus(roleId, status);
        return Result.judge(result);
    }

    @Operation(summary = "获取角色的菜单ID集合")
    @GetMapping("/{roleId}/menuIds")
    public Result<List<Long>> getRoleMenuIds(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<Long> menuIds = sysRoleService.getRoleMenuIds(roleId);
        return Result.success(menuIds);
    }

    @Operation(summary = "角色分配菜单权限")
    @PutMapping("/{roleId}/menus")
    public Result<Void> assignMenusToRole(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenusToRole(roleId, menuIds);
        return Result.success();
    }
}
