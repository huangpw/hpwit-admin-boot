package com.hpwit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hpwit.system.model.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 角色持久层接口
 *
 * @author Ray.Hao
 * @since 2022/1/14
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 获取最大范围的数据权限
     * @param roles 角色编码集合
     * @return
     */
    Integer getMaximumDataScope(Set<String> roles);

    Long getMinDataScopeByUserId(Long userId);

    /**
     * 根据用户ID查询角色编码列表
     */
    List<String> selectRoleCodeListByUserId(@Param("userId") Long userId);
    /**
     * 根据用户ID查询角色名称列表
     */
    List<String> selectRoleNameListByUserId(@Param("userId") Long userId);

}
