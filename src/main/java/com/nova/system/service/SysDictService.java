package com.nova.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nova.system.model.entity.SysDict;
import com.nova.system.model.form.SysDictForm;
import com.nova.system.model.query.SysDictQuery;

import java.util.List;

/**
 * 字典业务接口层
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:17
 */
public interface SysDictService extends IService<SysDict> {

    /** 获取字典库列表 */
    List<SysDict> getDictList(SysDictQuery queryParams);

    /** 根据Id修改状态 */
    boolean updateDictStatus(Long id, Boolean status);

    /** 新增字典 */
    boolean saveDict(SysDictForm formData);

    /** 修改字典 */
    boolean updateDict(Long id, SysDictForm dictForm);

    /** 根据字典ID列表获取字典编码列表 */
    List<String> getDictCodesByIds(List<String> ids);

    /** 删除字典 */
    boolean deleteDictByIds(List<String> ids);
}
