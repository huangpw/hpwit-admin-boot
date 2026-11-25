package com.hpwit.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hpwit.common.result.PageResult;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.form.SysNoticeForm;
import com.hpwit.system.model.query.SysNoticePageQuery;
import com.hpwit.system.model.vo.SysNoticeDetailVo;
import com.hpwit.system.model.vo.SysNoticePageVo;
import com.hpwit.system.model.vo.SysUserNoticePageVo;
import com.hpwit.system.service.SysNoticeService;
import com.hpwit.system.service.SysUserNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 通知公告前端控制层
 *
 * @author youlaitech
 * @since 2024-08-27 10:31
 */
@Tag(name = "09.通知公告")
@RestController
@RequestMapping("/v1/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService sysNoticeService;

    private final SysUserNoticeService sysUserNoticeService;

    @Operation(summary = "通知公告分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('sys:notice:query')")
    public PageResult<SysNoticePageVo> getNoticePage(SysNoticePageQuery queryParams) {
        IPage<SysNoticePageVo> result = sysNoticeService.getNoticePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增通知公告")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('sys:notice:add')")
    public Result<?> saveNotice(@RequestBody @Valid SysNoticeForm formData) {
        boolean result = sysNoticeService.saveNotice(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取通知公告表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('sys:notice:edit')")
    public Result<SysNoticeForm> getNoticeForm(
            @Parameter(description = "通知公告ID") @PathVariable Long id
    ) {
        SysNoticeForm formData = sysNoticeService.getNoticeFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "阅读获取通知公告详情")
    @GetMapping("/{id}/detail")
    public Result<SysNoticeDetailVo> getNoticeDetail(
            @Parameter(description = "通知公告ID") @PathVariable Long id
    ) {
        SysNoticeDetailVo detailVO = sysNoticeService.getNoticeDetail(id);
        return Result.success(detailVO);
    }

    @Operation(summary = "修改通知公告")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:notice:edit')")
    public Result<Void> updateNotice(
            @Parameter(description = "通知公告ID") @PathVariable Long id,
            @RequestBody @Validated SysNoticeForm formData
    ) {
        boolean result = sysNoticeService.updateNotice(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "发布通知公告")
    @PutMapping("/{id}/publish")
    //@PreAuthorize("@ss.hasPerm('sys:notice:publish')")
    public Result<Void> publishNotice(@Parameter(description = "通知公告ID") @PathVariable Long id) {
        boolean result = sysNoticeService.publishNotice(id);
        return Result.judge(result);
    }

    @Operation(summary = "撤回通知公告")
    @PutMapping("/{id}/revoke")
    //@PreAuthorize("@ss.hasPerm('sys:notice:revoke')")
    public Result<Void> revokeNotice(@Parameter(description = "通知公告ID") @PathVariable Long id) {
        boolean result = sysNoticeService.revokeNotice(id);
        return Result.judge(result);
    }

    @Operation(summary = "删除通知公告")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('sys:notice:delete')")
    public Result<Void> deleteNotices(
            @Parameter(description = "通知公告ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = sysNoticeService.deleteNotices(ids);
        return Result.judge(result);
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        sysUserNoticeService.readAll();
        return Result.success();
    }

    @Operation(summary = "获取我的通知公告分页列表")
    @GetMapping("/my")
    public PageResult<SysUserNoticePageVo> getMyNoticePage(
            SysNoticePageQuery queryParams
    ) {
        IPage<SysUserNoticePageVo> result = sysNoticeService.getMyNoticePage(queryParams);
        return PageResult.success(result);
    }
}
