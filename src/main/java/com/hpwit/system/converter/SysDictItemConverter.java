package com.hpwit.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hpwit.common.base.Option;
import com.hpwit.system.model.entity.SysDictItem;
import com.hpwit.system.model.form.SysDictItemForm;
import com.hpwit.system.model.vo.SysDictVo;
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
