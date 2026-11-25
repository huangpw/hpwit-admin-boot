package com.hpwit.system.converter;

import com.hpwit.system.model.entity.SysMenu;
import com.hpwit.system.model.form.SysMenuForm;
import com.hpwit.system.model.vo.SysMenuVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 菜单对象转换器
 *
 * @author Ray Hao
 * @since 2024/5/26
 */
@Mapper(componentModel = "spring")
public interface SysMenuConverter {

    SysMenuVo toVo(SysMenu entity);

    @Mapping(target = "params", ignore = true)
    SysMenuForm toForm(SysMenu entity);

    @Mapping(target = "params", ignore = true)
    SysMenu toEntity(SysMenuForm menuForm);

}