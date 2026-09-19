package com.nova.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nova.common.enums.LogModuleEnum;
import com.nova.common.result.PageResult;
import com.nova.common.result.Result;
import com.nova.core.annotation.Log;
import com.nova.system.model.form.SysConfigForm;
import com.nova.system.model.query.SysConfigPageQuery;
import com.nova.system.model.vo.SysConfigVo;
import com.nova.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置前端控制层
 * @author huangpw
 */
@Slf4j
@RestController
@Tag(name = "08.系统配置")
@RequestMapping("/v1/config")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    @Operation(summary = "系统配置分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('sys:config:query')")
    @Log( value = "系统配置分页列表",module = LogModuleEnum.SETTING)
    public PageResult<SysConfigVo> page(@ParameterObject SysConfigPageQuery configPageQuery) {
        IPage<SysConfigVo> result = sysConfigService.page(configPageQuery);
        return PageResult.success(result);
    }

    @Operation(summary = "新增系统配置")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('sys:config:add')")
    @Log( value = "新增系统配置",module = LogModuleEnum.SETTING)
    public Result<?> save(@RequestBody @Valid SysConfigForm configForm) {
        return Result.judge(sysConfigService.save(configForm));
    }

    @Operation(summary = "获取系统配置表单数据")
    @GetMapping("/{id}/form")
    public Result<SysConfigForm> getConfigForm(@Parameter(description = "系统配置ID") @PathVariable Long id) {
        SysConfigForm formData = sysConfigService.getConfigFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "刷新系统配置缓存")
    @PutMapping("/refresh")
    //@PreAuthorize("@ss.hasPerm('sys:config:refresh')")
    @Log( value = "刷新系统配置缓存",module = LogModuleEnum.SETTING)
    public Result<SysConfigForm> refreshCache() {
        return Result.judge(sysConfigService.refreshCache());
    }

    @Operation(summary = "修改系统配置")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:config:update')")
    @Log( value = "修改系统配置",module = LogModuleEnum.SETTING)
    public Result<?> update(@Valid @PathVariable Long id, @RequestBody SysConfigForm configForm) {
        return Result.judge(sysConfigService.edit(id, configForm));
    }

    @Operation(summary = "删除系统配置")
    @DeleteMapping("/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:config:delete')")
    @Log( value = "删除系统配置",module = LogModuleEnum.SETTING)
    public Result<?> delete(@PathVariable Long id) {
        return Result.judge(sysConfigService.delete(id));
    }

}
