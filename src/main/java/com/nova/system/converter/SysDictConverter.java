package com.nova.system.converter;

import com.nova.system.model.entity.SysDict;
import com.nova.system.model.form.SysDictForm;
import org.mapstruct.Mapper;

/**
 * 数据字典类型对象转换器
 * @Author huangpw
 * @Date 2026/1/12 21:51
 */
@Mapper(componentModel = "spring")
public interface SysDictConverter {

    SysDictForm toForm(SysDict entity);

    SysDict toEntity(SysDictForm formData);
}
