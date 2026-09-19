package com.nova.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nova.common.base.Option;
import com.nova.system.model.entity.SysRole;
import com.nova.system.model.form.SysRoleForm;
import com.nova.system.model.vo.SysRolePageVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 角色对象转换器
 *
 * @author haoxr
 * @since 2022/5/29
 */
@Mapper(componentModel = "spring")
public interface SysRoleConverter {

    Page<SysRolePageVo> toPageVo(Page<SysRole> page);

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "name")
    })
    Option<Long> toOption(SysRole role);

    List<Option<Long>> toOptions(List<SysRole> roles);

    SysRole toEntity(SysRoleForm roleForm);

    SysRoleForm toForm(SysRole entity);
}