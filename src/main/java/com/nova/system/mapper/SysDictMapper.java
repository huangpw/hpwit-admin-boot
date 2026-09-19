package com.nova.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nova.system.model.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典库访问层
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:14
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /** 获取字典库列表 */
    //List<SysDictVo> selectDictList(SysDictQuery queryParams);
}
