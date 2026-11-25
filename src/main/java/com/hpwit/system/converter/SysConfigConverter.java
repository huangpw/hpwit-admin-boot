package com.hpwit.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hpwit.system.model.entity.SysConfig;
import com.hpwit.system.model.form.SysConfigForm;
import com.hpwit.system.model.vo.SysConfigVo;
import org.mapstruct.Mapper;

/**
 * 系统配置对象转换器
 *
 * @author Theo
 * @since 2024-7-29 11:42:49
 */
@Mapper(componentModel = "spring")
public interface SysConfigConverter {

    Page<SysConfigVo> toPageVo(Page<SysConfig> page);

    SysConfig toEntity(SysConfigForm configForm);

    SysConfigForm toForm(SysConfig entity);
}
