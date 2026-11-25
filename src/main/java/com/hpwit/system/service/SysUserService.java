package com.hpwit.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hpwit.auth.model.form.LoginForm;
import com.hpwit.auth.model.vo.LoginVo;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.dto.CurrentUserDto;
import com.hpwit.system.model.entity.SysUser;
import com.hpwit.system.model.form.SysUserForm;
import com.hpwit.system.model.query.SysUserOnlinePageQuery;
import com.hpwit.system.model.query.SysUserPageQuery;
import com.hpwit.system.model.vo.SysUserExportVo;
import com.hpwit.system.model.vo.SysUserOnlineVo;
import com.hpwit.system.model.vo.SysUserPageVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理服务
 * @Author: AlbertHPW
 * @Date: 2025/11/22 11:22
 */
public interface SysUserService extends IService<SysUser> {

    /** 登录 */
    Result<LoginVo> login(LoginForm loginForm);

    /** 注册 */
    Result<?> register(SysUser user);

    /** 分页查询用户列表 */
    Page<SysUserPageVo> getUserPage(SysUserPageQuery query);

    /** 新增用户 */
    boolean saveUser(SysUserForm userForm);

    /** 获取用户表单数据 */
    SysUserForm getUserFormData(Long userId);

    /** 修改用户 */
    boolean updateUser(Long userId, SysUserForm userForm);

    /** 删除用户 */
    boolean deleteUsers(String idsStr);

    /** 获取登录用户信息 */
    CurrentUserDto getCurrentUserInfo();

    /** 导出用户列表 */
    List<SysUserExportVo> getExportUserList();

    /** 导入用户列表 */
    boolean importUserExcel(MultipartFile file, List<String> operationType);

    /** 重置密码 */
    Result<?> resetPassword(Long userId, String password);

    /** 用户列表 */
    List<SysUser> getUserList(SysUserPageQuery query);

    /** 在线用户列表 */
    List<SysUserOnlineVo> getUserOnlineList(SysUserOnlinePageQuery query);
}
