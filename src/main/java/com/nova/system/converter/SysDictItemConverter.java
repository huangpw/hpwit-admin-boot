package com.nova.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nova.common.base.Option;
import com.nova.system.model.entity.SysDictItem;
import com.nova.system.model.form.SysDictItemForm;
import com.nova.system.model.vo.SysDictVo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 字典项对象转换器
 *
 * @author Ray.Hao
 * @since 2022/6/8
 */
@Mapper(componentModel = "spring")
public interface SysDictItemConverter {

    Page<SysDictVo> toPageVo(Page<SysDictItem> page);

    SysDictItemForm toForm(SysDictItem entity);

    SysDictItem toEntity(SysDictItemForm formFata);

    Option<Long> toOption(SysDictItem dictItem);
    List<Option<Long>> toOption(List<SysDictItem> dictData);
}
