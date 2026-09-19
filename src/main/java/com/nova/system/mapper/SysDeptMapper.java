package com.nova.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.nova.system.model.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 部门管理 Mapper 接口
 * @author Huang
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    //@DataPermission(deptIdColumnName = "id") 数据权限
    @Override
    List<SysDept> selectList(@Param(Constants.WRAPPER) Wrapper<SysDept> queryWrapper);

    /**
     * 根据部门ID查询部门名称
     */
    String selectDeptNameById(@Param("deptId") Long deptId);

}
