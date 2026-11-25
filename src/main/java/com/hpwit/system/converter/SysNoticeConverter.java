package com.hpwit.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hpwit.system.model.bo.SysNoticeBo;
import com.hpwit.system.model.entity.SysNotice;
import com.hpwit.system.model.form.SysNoticeForm;
import com.hpwit.system.model.vo.SysNoticeDetailVo;
import com.hpwit.system.model.vo.SysNoticePageVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * 通知公告对象转换器
 *
 * @author huangpw
 */
@Mapper(componentModel = "spring")
public interface SysNoticeConverter {


    @Mappings({
            @Mapping(target = "targetUserIds", expression = "java(cn.hutool.core.util.StrUtil.split(entity.getTargetUserIds(),\",\"))")
    })
    SysNoticeForm toForm(SysNotice entity);

    @Mappings({
            @Mapping(target = "targetUserIds", expression = "java(cn.hutool.core.collection.CollUtil.join(formData.getTargetUserIds(),\",\"))")
    })
    SysNotice toEntity(SysNoticeForm formData);

    SysNoticePageVo toPageVo(SysNoticeBo bo);

    Page<SysNoticePageVo> toPageVo(Page<SysNoticeBo> noticePage);

    SysNoticeDetailVo toDetailVO(SysNoticeBo noticeBO);
}
