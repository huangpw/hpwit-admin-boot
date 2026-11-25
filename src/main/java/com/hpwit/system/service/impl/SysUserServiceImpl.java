package com.hpwit.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hpwit.auth.model.form.LoginForm;
import com.hpwit.auth.model.vo.LoginVo;
import com.hpwit.common.constant.RedisConstant;
import com.hpwit.common.constant.SysConstant;
import com.hpwit.common.enums.DataScopeEnum;
import com.hpwit.common.result.Result;
import com.hpwit.common.utils.PasswordUtil;
import com.hpwit.common.utils.RoleUtil;
import com.hpwit.system.converter.SysUserConverter;
import com.hpwit.system.mapper.*;
import com.hpwit.system.model.dto.CurrentUserDto;
import com.hpwit.system.model.entity.SysDept;
import com.hpwit.system.model.entity.SysLog;
import com.hpwit.system.model.entity.SysUser;
import com.hpwit.system.model.form.SysUserForm;
import com.hpwit.system.model.query.SysUserOnlinePageQuery;
import com.hpwit.system.model.query.SysUserPageQuery;
import com.hpwit.system.model.vo.SysUserExportVo;
import com.hpwit.system.model.vo.SysUserImportVo;
import com.hpwit.system.model.vo.SysUserOnlineVo;
import com.hpwit.system.model.vo.SysUserPageVo;
import com.hpwit.system.service.SysUserRoleService;
import com.hpwit.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: AlbertHPW
 * @Date: 2025/11/22 11:22
 */
@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SysUserConverter sysUserConverter; // 用户实体类转换器
    @Autowired
    private SysUserRoleService sysUserRoleService; // 用户和角色关联管理
    @Autowired
    private SysDeptMapper sysDeptMapper; // 部门管理
    @Autowired
    private SysRoleMapper sysRoleMapper; // 角色管理
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysLogMapper sysLogMapper;

    /** 用户登录 */
    @Override
    public Result<LoginVo> login(LoginForm loginForm) {
        // 查询用户是否存在
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, loginForm.getUsername())
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPassword, SysUser::getSalt, SysUser::getNickname, SysUser::getAvatar, SysUser::getDeptId);
        SysUser user = this.getOne(queryWrapper, false);
        if (user == null) {
            return Result.failed(SysConstant.LOGIN_USER_NOT_FOUND);
        }

        // 校验密码
        boolean verify = PasswordUtil.verifyPassword(loginForm.getPassword(), user.getPassword(), user.getSalt());
        if (!verify) {
            return Result.failed(SysConstant.LOGIN_PASSWORD_ERROR);
        }

        Long dataScope = Optional.ofNullable(sysRoleMapper.getMinDataScopeByUserId(user.getId()))
                .orElse(DataScopeEnum.SELF.getValue().longValue());

        // 登录成功
        StpUtil.login(user.getId());

        // 登录成功后删除图形验证码（防复用；密码错误等失败场景下保留，让用户可在 TTL 内重试）
        String captchaRedisKey = StrUtil.format(RedisConstant.Captcha.IMAGE_CODE, loginForm.getCaptchaKey());
        redisTemplate.delete(captchaRedisKey);

        // 返回结果
        LoginVo loginVo = new LoginVo();
        // 获取AccessToken
        String accessToken = StpUtil.getTokenValue();
        // 获取RefreshToken
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String refreshKey = "refresh_token:" + user.getId();
        redisTemplate.opsForValue().set(refreshKey, refreshToken, 7, TimeUnit.DAYS);

        loginVo.setId(user.getId());
        loginVo.setUserName(user.getUsername());
        loginVo.setNickname(user.getNickname());
        loginVo.setAccessToken(accessToken);
        loginVo.setRefreshToken(refreshToken);
        String avatar = Optional.ofNullable(user.getAvatar()).orElse("https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=nova-super");
        loginVo.setAvatar(avatar);

        // 获取角色
        List<String> roleList = StpUtil.getRoleList();
        loginVo.setRoles(roleList);
        // 获取权限
        List<String> permissionList = StpUtil.getPermissionList();
        loginVo.setPerms(permissionList);

        // 登录时存Session
        boolean isRoot = roleList.stream().anyMatch(r -> r.equalsIgnoreCase(SysConstant.ROOT_ROLE_CODE));
        SaSession session = StpUtil.getSession();
        session.set("isRoot", isRoot);
        if(!isRoot) {
            // 非超级管理员
            session.set("userId", user.getId());
            if (user.getDeptId() == null) {
                session.delete("deptId");
            } else {
                session.set("deptId", user.getDeptId());
            }
            session.set("dataScope", dataScope);
        }

        return Result.success(loginVo);
    }

    /** 注册 */
    @Override
    public Result<?> register(SysUser user) {

        // 密码加密
        user.setSalt(PasswordUtil.generateSalt());
        user.setPassword(PasswordUtil.hashPassword(user.getPassword(), user.getSalt()));

        int isSave = this.baseMapper.insert(user);

        return  isSave > 0 ? Result.success(SysConstant.REGISTER_SUCCESS) : Result.failed(SysConstant.REGISTER_FAILED);
    }

    /** 分页查询用户列表 */
    @Override
    public Page<SysUserPageVo> getUserPage(SysUserPageQuery query) {
        try {
            // 分页参数
            Page<SysUserPageVo> page = new Page<>(query.getPageNum(), query.getPageSize());

            boolean isRoot = RoleUtil.isRoot();
            query.setIsRoot(isRoot);

            return this.baseMapper.getUserPage(page, query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 新增用户 */
    @Override
    public boolean saveUser(SysUserForm userForm) {
        String username = userForm.getUsername();

        long count = this.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        Assert.isTrue(count == 0, SysConstant.REGISTER_USER_EXISTS);

        // 实体转换 form->entity
        SysUser entity = sysUserConverter.toEntity(userForm);

        // 设置默认加密密码
        String salt = PasswordUtil.generateSalt();
        String encryptPwd = PasswordUtil.hashPassword(userForm.getPassword(), salt);
        entity.setSalt(salt);
        entity.setPassword(encryptPwd);
        if(StpUtil.getLoginId() != null) {
            entity.setCreateBy(Long.valueOf(StpUtil.getLoginId().toString()));
        }

        // 新增用户
        boolean result = this.save(entity);

        if (result) {
            // 保存用户角色
            sysUserRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 获取用户表单数据
     * @param userId 用户ID
     * @return {@link SysUserForm} 用户表单数据
     */
    @Override
    public SysUserForm getUserFormData(Long userId) {
        return this.baseMapper.getUserFormData(userId);
    }

    /**
     * 更新用户
     * @param userId   用户ID
     * @param userForm 用户表单对象
     * @return true|false
     */
    @Override
    @Transactional
    public boolean updateUser(Long userId, SysUserForm userForm) {

        String username = userForm.getUsername();

        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(SysUser::getId, userId)
        );
        Assert.isTrue(count == 0, "用户名已存在");

        // form -> entity
        SysUser entity = sysUserConverter.toEntity(userForm);
        entity.setId(userId);
        // 未填写密码时保留原密码，避免转换器的默认明文覆盖数据库哈希。
        if (StrUtil.isBlank(userForm.getPassword())) {
            entity.setPassword(null);
            entity.setSalt(null);
        } else {
            String salt = PasswordUtil.generateSalt();
            entity.setSalt(salt);
            entity.setPassword(PasswordUtil.hashPassword(userForm.getPassword(), salt));
        }
        entity.setUpdateBy(Long.valueOf(StpUtil.getLoginId().toString()));

        // 修改用户
        boolean result = this.updateById(entity);

        if (result) {
            // 保存用户角色
            sysUserRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 删除用户
     * @param idsStr 用户ID，多个以英文逗号(,)分割
     * @return true|false
     */
    @Override
    public boolean deleteUsers(String idsStr) {
        Assert.isTrue(StrUtil.isNotBlank(idsStr), "删除的用户数据为空");
        // 逻辑删除
        List<Long> ids = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return this.removeByIds(ids);
    }

    /**
     * 获取登录用户信息
     * @return {@link CurrentUserDto}   用户信息
     */
    @Override
    public CurrentUserDto getCurrentUserInfo() {

        Long userId = Long.valueOf(StpUtil.getLoginIdAsString());

        // 获取登录用户基础信息
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .select(
                        SysUser::getId,
                        SysUser::getUsername,
                        SysUser::getNickname,
                        SysUser::getAvatar
                )
        );
        // entity->VO
        CurrentUserDto userInfoVo = sysUserConverter.toCurrentUserDto(user);

        // 用户角色集合
        Set<String> roles = new HashSet<>(StpUtil.getRoleList(userId));
        userInfoVo.setRoles(roles);

        // 用户权限集合
        if (CollectionUtil.isNotEmpty(roles)) {
            //Set<String> perms = permissionService.getRolePermsFormCache(roles);
            // TODO 从缓存中获取
            HashSet<String> perms = new HashSet<>(StpUtil.getPermissionList(userId));
            userInfoVo.setPerms(perms);
        }
        return userInfoVo;
    }

    /** 导出用户列表 */
    @Override
    public List<SysUserExportVo> getExportUserList() {
        List<SysUser> list = this.baseMapper.selectList(null);
        return list.stream().map(user -> {
            return sysUserConverter.toExportVo(user);
        }).toList();
    }

    /** 导入用户列表 */
    @Override
    public boolean importUserExcel(MultipartFile file, List<String> operationType) {

      try {
          List<SysUserImportVo> list = EasyExcel.read(file.getInputStream(), SysUserImportVo.class, null)
                  .sheet()
                  .doReadSync();// 同步读取

          if(list.isEmpty()) {
              return false;
          }

          for (SysUserImportVo vo : list) {
              if(StrUtil.isNotBlank(vo.getUsername())) {

                  Long deptId = null;
                  if(StrUtil.isNotBlank(vo.getDeptName())) {
                      SysDept dept = sysDeptMapper.selectOne(new LambdaQueryWrapper<SysDept>().eq(SysDept::getName, vo.getDeptName()));
                      if(dept != null) {
                          deptId = dept.getId();
                      }
                  }

                  LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
                  queryWrapper.eq(SysUser::getUsername, vo.getUsername()).last("limit 1");
                  SysUser user = this.getOne(queryWrapper);
                  if (user == null && operationType.contains("add")) {
                      // 新增用户信息
                      SysUser entity = sysUserConverter.toEntity(vo);
                      entity.setDeptId(deptId);
                      String salt = PasswordUtil.generateSalt();
                      entity.setSalt(salt);
                      String password = PasswordUtil.hashPassword(SysConstant.DEFAULT_PASSWORD, salt);
                      entity.setPassword(password);
                      entity.setCreateBy(Long.valueOf(StpUtil.getLoginId().toString()));
                      this.save(entity);
                  }

                  if (user != null && operationType.contains("update")) {
                      // 更新用户信息
                      SysUser entity = sysUserConverter.toEntity(vo);
                      entity.setId(user.getId());
                      entity.setDeptId(deptId);
                      entity.setUpdateBy(Long.valueOf(StpUtil.getLoginId().toString()));
                      this.updateById(entity);
                  }
              }
          }
          return true;
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
    }

    /** 重置密码 */
    @Override
    public Result<?> resetPassword(Long userId, String password) {
        String salt = PasswordUtil.generateSalt();
        String encryptPwd = PasswordUtil.hashPassword(password, salt);

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                .set(SysUser::getSalt, salt)
                .set(SysUser::getPassword, encryptPwd);
        boolean isUpdate = this.update(updateWrapper);
        return Result.judge(isUpdate);
    }

    /** 用户列表 */
    @Override
    public List<SysUser> getUserList(SysUserPageQuery query) {
        return this.list();
    }

    /** 在线用户列表 */
    @Override
    public List<SysUserOnlineVo> getUserOnlineList(SysUserOnlinePageQuery query) {

        List<SysUserOnlineVo> list = new ArrayList<>();
        Set<Long> seenLoginIds = new HashSet<>(); // 用来记录已经处理过的 loginId

        // 1. 获取所有在线 token
        List<String> tokenList = StpUtil.searchTokenValue("", 0, -1, true); // sort 是否排序（true = 新 → 旧）
        for (String token : tokenList) {
            // token 已过期的跳过
            String newToken = token.replaceAll("Authorization:login:token:", "");
            long timeout = StpUtil.getTokenTimeout(newToken);
            if (timeout == -2) {
                continue;
            }

            Object userId = StpUtil.getLoginIdByToken(newToken);
            //Long loginId = Long.valueOf(userId.toString());

            // 已经存在就跳过
            //if (seenLoginIds.contains(userId.toString())) {
            //    continue;
            //}
            //seenLoginIds.add(userId.toString());

            SysUserOnlineVo vo = new SysUserOnlineVo();
            vo.setLoginId(userId);
            vo.setToken(newToken);

            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getId, userId)
                    .select(SysUser::getUsername, SysUser::getDeptId);
            SysUser user = this.getOne(queryWrapper);
            if(user != null) {
                vo.setUsername(user.getUsername());
                SysDept dept = sysDeptMapper.selectById(user.getDeptId());
                if(dept != null) {
                    vo.setDeptName(dept.getName());
                }

                log.info("用户 {} 在线", user.getUsername());

                SysLog sysLog = sysLogMapper.selectLatestByRequestParams(user.getUsername());
                if(sysLog != null) {
                    vo.setIp(sysLog.getIp());
                    vo.setLoginLocation(sysLog.getProvince() + sysLog.getCity());
                    vo.setOs(sysLog.getOs());
                    vo.setBrowser(sysLog.getBrowser());
                    vo.setCreateTime(sysLog.getCreateTime());
                }
            }

            list.add(vo);
        }
        return list;
    }

}
