package com.hpwit.system.controller;

import com.hpwit.common.base.Option;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.form.SysDeptForm;
import com.hpwit.system.model.query.SysDeptQuery;
import com.hpwit.system.model.vo.SysDeptVo;
import com.hpwit.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门控制器
 *
 * @author Ray.Hao
 * @since 2020/11/6
 */
@Tag(name = "05.部门接口")
@RestController
@RequestMapping("/v1/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService sysDeptService;

    @Operation(summary = "部门列表")
    @GetMapping("/list")
    //@Log( value = "部门列表",module = LogModuleEnum.DEPT)
    public Result<List<SysDeptVo>> getDeptList(SysDeptQuery query) {
        List<SysDeptVo> list = sysDeptService.getDeptList(query);
        return Result.success(list);
    }

    @Operation(summary = "部门下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> getDeptOptions() {
        List<Option<Long>> list = sysDeptService.listDeptOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增部门")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('sys:dept:add')")
    //@RepeatSubmit
    public Result<?> saveDept(@Valid @RequestBody SysDeptForm formData) {
        Long id = sysDeptService.saveDept(formData);
        return Result.success(id);
    }

    @Operation(summary = "获取部门表单数据")
    @GetMapping("/{deptId}/form")
    public Result<SysDeptForm> getDeptForm(@Parameter(description ="部门ID") @PathVariable Long deptId) {
        SysDeptForm deptForm = sysDeptService.getDeptForm(deptId);
        return Result.success(deptForm);
    }

    @Operation(summary = "修改部门")
    @PutMapping(value = "/{deptId}")
    //@PreAuthorize("@ss.hasPerm('sys:dept:edit')")
    public Result<?> updateDept(@PathVariable Long deptId, @Valid @RequestBody SysDeptForm formData) {
        deptId = sysDeptService.updateDept(deptId, formData);
        return Result.success(deptId);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('sys:dept:delete')")
    public Result<?> deleteDepartments(@Parameter(description ="部门ID，多个以英文逗号(,)分割") @PathVariable("ids") String ids) {
        boolean result = sysDeptService.deleteByIds(ids);
        return Result.judge(result);
    }

}
