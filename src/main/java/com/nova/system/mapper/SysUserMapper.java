package com.nova.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nova.core.annotation.DataPermission;
import com.nova.system.model.entity.SysUser;
import com.nova.system.model.form.SysUserForm;
import com.nova.system.model.query.SysUserPageQuery;
import com.nova.system.model.vo.SysUserPageVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户管理业务接口
 * @Author: AlbertHPW
 * @Date: 2025/11/22 11:24
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 获取用户分页列表
     * @param page        分页参数
     * @param query 查询参数
     * @return 用户分页列表
     */
    @DataPermission(deptAlias = "u", userAlias = "u")
    Page<SysUserPageVo> getUserPage(Page<SysUserPageVo> page, SysUserPageQuery query);

    /**
     * 获取用户表单详情
     * @param userId 用户ID
     * @return 用户表单详情
     */
    SysUserForm getUserFormData(Long userId);
}
