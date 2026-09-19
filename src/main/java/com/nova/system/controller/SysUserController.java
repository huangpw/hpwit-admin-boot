package com.nova.system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nova.common.result.PageResult;
import com.nova.common.result.Result;
import com.nova.system.converter.SysUserConverter;
import com.nova.system.model.dto.CurrentUserDto;
import com.nova.system.model.entity.SysUser;
import com.nova.system.model.form.SysUserForm;
import com.nova.system.model.query.SysUserOnlinePageQuery;
import com.nova.system.model.query.SysUserPageQuery;
import com.nova.system.model.vo.SysUserExportVo;
import com.nova.system.model.vo.SysUserOnlineVo;
import com.nova.system.model.vo.SysUserPageVo;
import com.nova.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户管理控制层
 *
 * @Author: AlbertHPW
 * @Date: 2026/1/6 22:45
 */
@Tag(name = "02.用户接口")
@RestController
@RequestMapping("/v1/user")
@Slf4j
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysUserConverter sysUserConverter;

    @Operation(summary = "用户分页列表")
    @GetMapping("/page")
    //@Log(value = "用户分页列表", module = LogModuleEnum.USER)
    public PageResult<SysUserPageVo> getUserPage(SysUserPageQuery query) {
        IPage<SysUserPageVo> result = sysUserService.getUserPage(query);
        return PageResult.success(result);
    }

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public Result<?> getUserList(SysUserPageQuery query) {
        List<SysUser> result = sysUserService.getUserList(query);
        return Result.success(result);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    //@SaCheckPermission("sys:user:add")
    public Result<?> saveUser(@RequestBody @Valid SysUserForm userForm) {
        boolean result = sysUserService.saveUser(userForm);
        return Result.judge(result);
    }

    @Operation(summary = "获取用户表单数据")
    @GetMapping("/{userId}/form")
    //@PreAuthorize("@ss.hasPerm('sys:user:edit')")
    //@Log(value = "用户表单数据", module = LogModuleEnum.USER)
    public Result<SysUserForm> getUserForm(@Parameter(description = "用户ID") @PathVariable Long userId) {
        SysUserForm formData = sysUserService.getUserFormData(userId);
        return Result.success(formData);
    }

    @Operation(summary = "修改用户")
    @PutMapping(value = "/{userId}")
    //@PreAuthorize("@ss.hasPerm('sys:user:edit')")
    //@Log(value = "修改用户", module = LogModuleEnum.USER)
    public Result<Void> updateUser(@Parameter(description = "用户ID") @PathVariable Long userId,
                                   @RequestBody @Valid SysUserForm userForm) {
        boolean result = sysUserService.updateUser(userId, userForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('sys:user:delete')")
    //@Log(value = "删除用户", module = LogModuleEnum.USER)
    public Result<Void> deleteUsers(@Parameter(description = "用户ID，多个以英文逗号(,)分割") @PathVariable String ids) {
        boolean result = sysUserService.deleteUsers(ids);
        return Result.judge(result);
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping(value = "/{userId}/status")
    //@PreAuthorize("@ss.hasPerm('sys:user:edit')")
    //@Log(value = "修改用户状态", module = LogModuleEnum.USER)
    public Result<Void> updateUserStatus(@Parameter(description = "用户ID") @PathVariable Long userId,
                                         @Parameter(description = "用户状态(1:启用;0:禁用)") @RequestParam Integer status) {
        boolean result = sysUserService.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getStatus, status)
        );
        return Result.judge(result);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    //@Log(value = "获取当前登录用户信息", module = LogModuleEnum.USER)
    public Result<CurrentUserDto> getCurrentUser() {
        CurrentUserDto currentUserDTO = sysUserService.getCurrentUserInfo();
        return Result.success(currentUserDTO);
    }

    @Operation(summary = "用户导入模板下载")
    @GetMapping("/template")
    //@Log(value = "用户导入模板下载", module = LogModuleEnum.USER)
    public void downloadTemplate(HttpServletResponse response) {
        String fileName = "用户导入模板.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName,
                StandardCharsets.UTF_8));

        String fileClassPath = "templates" + File.separator + "excel" + File.separator + fileName; // 文件地址
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileClassPath);

        try (ServletOutputStream outputStream = response.getOutputStream();
             ExcelWriter excelWriter = EasyExcel.write(outputStream).withTemplate(inputStream).build()) {
            excelWriter.finish();
        } catch (IOException e) {
            throw new RuntimeException("用户导入模板下载失败", e);
        }
    }

    @Operation(summary = "用户导入")
    @PostMapping("/import")
    //@Log(value = "用户导入", module = LogModuleEnum.USER)
    public Result<?> importUser(@RequestParam("file") MultipartFile file, @RequestParam("operationType") List<String> operationType) {
        try {
            boolean excel = sysUserService.importUserExcel(file, operationType);
            return Result.judge(excel);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    @Operation(summary = "用户导出")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {

        try {
            // 设置响应格式，在浏览器中下载Excel文件（.xlsx格式）
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode("用户列表", "UTF-8") + ".xlsx");

            // 获取用户列表
            List<SysUserExportVo> userList = sysUserService.getExportUserList();

            EasyExcel.write(response.getOutputStream(), SysUserExportVo.class)
                    .sheet("用户信息")
                    .doWrite(userList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/reset-password")
    public Result<?> resetPassword(@Parameter(description = "用户ID") @RequestParam Long userId, @Parameter(description = "新密码") @RequestParam String password) {
        return sysUserService.resetPassword(userId, password);
    }

    @Operation(summary = "在线用户列表")
    @GetMapping("/online")
    public Result<?> getUserOnlineList(SysUserOnlinePageQuery query) {
        List<SysUserOnlineVo> userList = sysUserService.getUserOnlineList(query);
        return Result.success(userList);
    }

}
