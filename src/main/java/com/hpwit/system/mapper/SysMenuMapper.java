package com.hpwit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hpwit.system.model.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Set;

/**
 * 菜单访问层
 *
 * @author Ray
 * @since 2022/1/24
 */

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询按钮权限标识列表
     */
    List<String> selectPermListByUserId(@Param("userId") Long userId);

    /**
     * 获取菜单路由列表
     *
     * @param roleCodes 角色编码集合
     */
    List<SysMenu> getMenusByRoleCodes(Set<String> roleCodes);

}
