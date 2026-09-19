package com.nova.system.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nova.core.exception.BusinessException;
import com.nova.system.converter.SysDictConverter;
import com.nova.system.mapper.SysDictMapper;
import com.nova.system.model.entity.SysDict;
import com.nova.system.model.entity.SysDictItem;
import com.nova.system.model.form.SysDictForm;
import com.nova.system.model.query.SysDictQuery;
import com.nova.system.service.SysDictItemService;
import com.nova.system.service.SysDictService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典业务实现类
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:18
 */
@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {

    @Autowired
    private SysDictConverter sysDictConverter;
    @Autowired
    private SysDictItemService sysDictItemService;

    /** 获取字典库列表 */
    @Override
    public List<SysDict> getDictList(SysDictQuery queryParams) {
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<>();

        // 判断 keywordValue 不为空才添加模糊查询
        String value = queryParams.getKeywordValue();
        if(StringUtils.isNotBlank(value)) {
            queryWrapper.and(qw -> {
                qw.like(SysDict::getName, value)
                        .or()
                        .like(SysDict::getDictCode, value);
            });
        }
        return this.list(queryWrapper);
    }

    /** 根据Id修改状态 */
    @Override
    public boolean updateDictStatus(Long id, Boolean status) {
        LambdaUpdateWrapper<SysDict> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysDict::getId, id)
                .set(SysDict::getStatus, status ? 1 : 0);
        return this.update(updateWrapper);
    }

    /** 新增字典 */
    @Override
    public boolean saveDict(SysDictForm formData) {

        SysDict dict = new SysDict();
        BeanUtils.copyProperties(formData, dict);

        return this.save(dict);
    }

    /** 更新字典 */
    @Override
    public boolean updateDict(Long id, SysDictForm dictForm) {
        // 获取字典
        SysDict entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("分类不存在");
        }
        // 校验 code 是否唯一
        String dictCode = dictForm.getDictCode();
        if (!entity.getDictCode().equals(dictCode)) {
            long count = this.count(new LambdaQueryWrapper<SysDict>()
                    .eq(SysDict::getDictCode, dictCode)
            );
            Assert.isTrue(count == 0, "分类编码已存在");
        }
        // 更新字典
        SysDict dict = sysDictConverter.toEntity(dictForm);
        dict.setId(id);
        boolean result = this.updateById(dict);
        if(result) {
            // 更新字典数据
            List<SysDictItem> dictItemList = sysDictItemService.list(
                    new LambdaQueryWrapper<SysDictItem>()
                            .eq(SysDictItem::getDictCode, entity.getDictCode())
                            .select(SysDictItem::getId)
            );
            if (!dictItemList.isEmpty()){
                List<Long> dictItemIds = dictItemList.stream().map(SysDictItem::getId).toList();
                SysDictItem dictItem = new SysDictItem();
                dictItem.setDictCode(dict.getDictCode()); // 更新字典编码(分类编码)
                sysDictItemService.update(dictItem,
                        new LambdaQueryWrapper<SysDictItem>()
                                .in(SysDictItem::getId, dictItemIds)
                );
            }
        }
        return result;
    }

    /** 根据字典ID列表获取字典编码列表 */
    @Override
    public List<String> getDictCodesByIds(List<String> ids) {
        List<SysDict> dictList = this.listByIds(ids);
        return dictList.stream().map(SysDict::getDictCode).toList();
    }

    /** 删除字典 */
    @Override
    public boolean deleteDictByIds(List<String> ids) {
        // 删除字典(分类)
        this.removeByIds(ids);

        // 删除字典项(分类属性)
        List<SysDict> list = this.listByIds(ids);
        if (!list.isEmpty()) {
            List<String> dictCodes = list.stream().map(SysDict::getDictCode).toList();
            sysDictItemService.remove(new LambdaQueryWrapper<SysDictItem>()
                    .in(SysDictItem::getDictCode, dictCodes)
            );
        }

        return true;
    }
}
