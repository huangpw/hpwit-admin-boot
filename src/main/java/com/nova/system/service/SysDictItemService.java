package com.nova.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nova.common.base.Option;
import com.nova.system.model.entity.SysDictItem;
import com.nova.system.model.form.SysDictItemForm;
import com.nova.system.model.query.SysDictItemQuery;

import java.util.List;

/**
 * 字典数据接口
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:19
 */
public interface SysDictItemService extends IService<SysDictItem> {

    /** 获取字典数据列表 */
    List<SysDictItem> getDictItemList(SysDictItemQuery queryParams);

    /** 根据分类编码获取字典数据列表 */
    List<SysDictItem> getDictItemListByCode(String dictCode, Boolean status);

    /**
     * 根据分类编码获取下拉选项列表（仅启用项，走 Redis 缓存）。
     *
     * <p>缓存策略：Cache-Aside + 写时主动失效 + 版本号变更信号 + TTL 兜底。
     * 调用方无需感知缓存，任意写操作（新增/修改/删除/启停）都会自动刷新缓存。</p>
     *
     * @param dictCode 字典编码
     * @return 下拉选项列表（值 = SysDictItem.value，标签 = SysDictItem.label）
     */
    List<Option<String>> getDictOptions(String dictCode);

    /** 根据Id修改状态 */
    Boolean updateDictItemStatus(Long id, Boolean status);

    /** 保存字典数据(分类属性) */
    boolean saveDictItem(SysDictItemForm formData);

    /** 获取字典数据表单 */
    SysDictItemForm getDictItemForm(Long itemId);

    /** 更新字典数据 */
    boolean updateDictItem(SysDictItemForm formData);

    /** 批量删除字典数据 */
    boolean deleteByIds(String ids);

    /**
     * 主动刷新指定字典编码的缓存（删除缓存 + 版本号 +1）。
     * <p>适用于：① 管理后台"清除缓存"按钮；② 数据修复后强制刷新；③ 接入外部数据源变更通知。</p>
     *
     * @param dictCode 字典编码
     */
    void refreshDictCache(String dictCode);

    /**
     * 清空所有字典项的 options 缓存与版本号（慎用，会一次性扫所有 keys）。
     */
    void refreshAllDictCache();
}
