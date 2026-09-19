package com.nova.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.nova.auth.model.form.LoginForm;
import com.nova.auth.model.vo.LoginVo;
import com.nova.common.result.Result;
import com.nova.common.utils.PasswordUtil;
import com.nova.common.constant.SysConstant;
import com.nova.system.converter.SysUserConverter;
import com.nova.system.model.form.SysUserForm;
import com.nova.system.service.SysUserRoleService;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import com.nova.system.mapper.SysRoleMapper;
import com.nova.system.model.entity.SysUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SysUserServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test"), SysUser.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "new-password"})
    void userEditsPreserveOrHashPassword(String password) {
        SysUserServiceImpl service = spy(new SysUserServiceImpl());
        ReflectionTestUtils.setField(service, "sysUserConverter", Mappers.getMapper(SysUserConverter.class));
        SysUserRoleService roleService = mock(SysUserRoleService.class);
        ReflectionTestUtils.setField(service, "sysUserRoleService", roleService);
        doReturn(0L).when(service).count(any());
        doReturn(true).when(service).updateById(any(SysUser.class));

        SysUserForm form = new SysUserForm();
        form.setUsername("edit-test");
        form.setNickname("Updated name");
        form.setPassword(password);
        form.setRoleIds(List.of(2L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(1L);
            assertTrue(service.updateUser(42L, form));
        }

        ArgumentCaptor<SysUser> updated = ArgumentCaptor.forClass(SysUser.class);
        verify(service).updateById(updated.capture());
        SysUser entity = updated.getValue();
        assertEquals(42L, entity.getId());
        assertEquals("Updated name", entity.getNickname());
        if (password == null || password.isBlank()) {
            assertNull(entity.getPassword());
            assertNull(entity.getSalt());
        } else {
            assertNotEquals(password, entity.getPassword());
            assertEquals(60, entity.getPassword().length());
            assertTrue(entity.getPassword().startsWith(entity.getSalt()));
            assertTrue(PasswordUtil.verifyPassword(password, entity.getPassword(), entity.getSalt()));
        }
        verify(roleService).saveUserRoles(42L, form.getRoleIds());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "password: $2a$10$invalid"})
    void loginRejectsMalformedStoredPassword(String storedPassword) {
        SysUserServiceImpl service = spy(new SysUserServiceImpl());
        SysUser user = new SysUser();
        user.setPassword(storedPassword);
        doReturn(user).when(service).getOne(any(), eq(false));
        LoginForm form = new LoginForm();
        form.setUsername("login-test");
        form.setPassword("123456");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            Result<LoginVo> result = service.login(form);
            assertFalse(Result.isSuccess(result));
            assertEquals(SysConstant.LOGIN_PASSWORD_ERROR, result.getMessage());
            stp.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, 7, false, 4",
            "3, 7, false, 3",
            "NULL, NULL, false, 4",
            "3, NULL, false, 3",
            "NULL, NULL, true, NULL"
    }, nullValues = "NULL")
    @SuppressWarnings("unchecked")
    void loginHandlesMissingScopeAndDepartment(Long scope, Long deptId, boolean root, Long expectedScope) {
        SysUserServiceImpl service = spy(new SysUserServiceImpl());
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        ReflectionTestUtils.setField(service, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        when(redis.opsForValue()).thenReturn(values);
        when(roleMapper.getMinDataScopeByUserId(42L)).thenReturn(scope);

        SysUser user = new SysUser();
        user.setId(42L);
        user.setUsername("login-test");
        user.setDeptId(deptId);
        user.setSalt(PasswordUtil.generateSalt());
        user.setPassword(PasswordUtil.hashPassword("test-password", user.getSalt()));
        doReturn(user).when(service).getOne(any(), eq(false));

        LoginForm form = new LoginForm();
        form.setUsername(user.getUsername());
        form.setPassword("test-password");
        SaSession session = new SaSession("login-test");
        session.set("deptId", 99L);
        List<String> roles = root ? List.of("root") : List.of();

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getSession).thenReturn(session);
            stp.when(StpUtil::getTokenValue).thenReturn("test-token");
            stp.when(StpUtil::getRoleList).thenReturn(roles);
            stp.when(StpUtil::getPermissionList).thenReturn(List.of());

            Result<LoginVo> result = service.login(form);

            assertTrue(Result.isSuccess(result));
            assertEquals("test-token", result.getData().getAccessToken());
            assertEquals(roles, result.getData().getRoles());
            assertEquals(root, session.get("isRoot"));
            assertEquals(expectedScope, session.get("dataScope"));
            if (!root) {
                assertEquals(42L, session.get("userId"));
                assertEquals(deptId, session.get("deptId"));
            }
            stp.verify(() -> StpUtil.login(42L));
        }
    }
}
