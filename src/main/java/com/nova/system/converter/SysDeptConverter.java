package com.nova.system.converter;

import com.nova.system.model.entity.SysDept;
import com.nova.system.model.form.SysDeptForm;
import com.nova.system.model.vo.SysDeptVo;
import org.mapstruct.Mapper;

/**
 * 部门对象转换器
 *
 * @author haoxr
 * @since 2022/7/29
 */
@Mapper(componentModel = "spring")
public interface SysDeptConverter {

    SysDeptForm toForm(SysDept entity);

    SysDeptVo toVo(SysDept entity);

    SysDept toEntity(SysDeptForm deptForm);

}