package com.hpwit.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hpwit.common.base.Option;
import com.hpwit.common.constant.RedisConstant;
import com.hpwit.system.converter.SysDictItemConverter;
import com.hpwit.system.mapper.SysDictItemMapper;
import com.hpwit.system.model.entity.SysDictItem;
import com.hpwit.system.model.form.SysDictItemForm;
import com.hpwit.system.model.query.SysDictItemQuery;
import com.hpwit.system.service.SysDictItemService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 字典数据实现类
 *
 * <p><b>缓存策略（Cache-Aside + 版本号）</b></p>
 * <ul>
 *   <li>读：先查 Redis，命中直接返回；未命中查 DB，回写 Redis（TTL 1h 兜底）。</li>
 *   <li>写：删除 options 缓存 + INCR version 计数器，作为"数据已变更"信号。</li>
 *   <li>外部监测：可通过 Redis {@code MONITOR}、{@code SUBSCRIBE} 或 {@code GET system:dict:version:{code}}
 *       获取版本变化，进而判断数据是否发生过变更。</li>
 * </ul>
 *
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:20
 */
@Slf4j
@Service
public class SysDictItemServiceImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements SysDictItemService {

    /** options 缓存 TTL，1 小时。即使写时失效逻辑漏了也能自动恢复 */
    private static final Duration DICT_CACHE_TTL = Duration.ofHours(1);

    @Autowired
    private SysDictItemConverter sysDictItemConverter;
    @Autowired
    private SysDictItemMapper sysDictItemMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 获取字典数据列表 */
    @Override
    public List<SysDictItem> getDictItemList(SysDictItemQuery queryParams) {
        LambdaQueryWrapper<SysDictItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDictItem::getDictCode, queryParams.getDictCode());

        // 判断 keywordValue 不为空才添加模糊查询
        String value = queryParams.getKeywordValue();
        if(StringUtils.isNotBlank(value)) {
            queryWrapper.and(qw -> {
                qw.like(SysDictItem::getLabel, value)
                        .or()
                        .like(SysDictItem::getValue, value);
            });
        }
        return this.list(queryWrapper);
    }

    /** 根据分类编码获取字典数据列表 */
    @Override
    public List<SysDictItem> getDictItemListByCode(String dictCode, Boolean status) {

        LambdaQueryWrapper<SysDictItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDictItem::getDictCode, dictCode);
        queryWrapper.orderByAsc(SysDictItem::getSort);

        if (Boolean.TRUE.equals(status)) {
            // 👉 status = true
            queryWrapper.eq(SysDictItem::getStatus, 1);
        }
        if (Boolean.FALSE.equals(status)) {
            // 👉 status = false
            queryWrapper.eq(SysDictItem::getStatus, 0);
        }

        return this.list(queryWrapper);
    }

    /**
     * 根据分类编码获取下拉选项列表（仅启用项，走 Redis 缓存）。
     *
     * @param dictCode 字典编码
     * @return 下拉选项列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Option<String>> getDictOptions(String dictCode) {
        if (StringUtils.isBlank(dictCode)) {
            return Collections.emptyList();
        }
        String cacheKey = StrUtil.format(RedisConstant.System.DICT_OPTIONS, dictCode);
        String versionKey = StrUtil.format(RedisConstant.System.DICT_VERSION, dictCode);

        // 1. 读缓存；反序列化失败视为脏数据（旧格式/损坏），删除后回源 DB 自愈
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
        } catch (SerializationException e) {
            log.warn("[dict-cache] 缓存反序列化失败，已删除脏缓存回源 DB dictCode={}", dictCode, e);
            redisTemplate.delete(cacheKey);
            cached = null;
        }
        if (cached instanceof List<?> list) {
            log.debug("[dict-cache] HIT dictCode={}, size={}", dictCode, list.size());
            return (List<Option<String>>) cached;
        }

        // 2. 未命中:查 DB,只取启用项(status=1),按 sort 升序
        // 注意:必须回写可变 ArrayList。Stream.toList() 返回 JDK 不可变列表
        // (ImmutableCollections$ListN),GenericJackson2JsonRedisSerializer 对 java.* 包下的
        // final 类型不写类型信息,写进去的值命中时自己读不出来(MismatchedInputException)
        List<Option<String>> options = new ArrayList<>(getDictItemListByCode(dictCode, true).stream()
                .map(it -> {
                    Option<String> opt = new Option<>(it.getValue(), it.getLabel());
                    opt.setTag(it.getTagType());
                    return opt;
                })
                .toList());

        // 3. 回写 Redis(TTL 兜底) + 初始化 version(若不存在)
        redisTemplate.opsForValue().set(cacheKey, options, DICT_CACHE_TTL);
        redisTemplate.opsForValue().setIfAbsent(versionKey, 0L, Duration.ofDays(30));
        log.debug("[dict-cache] MISS dictCode={}, 从 DB 加载并写入缓存 size={}", dictCode, options.size());
        return options;
    }

    /**
     * 主动刷新指定字典编码的缓存：删除 options + 版本号 +1。
     * <p>被所有写操作（新增/修改/删除/启停）调用，保证下一次读取时一定拉到最新数据。</p>
     *
     * @param dictCode 字典编码
     */
    @Override
    public void refreshDictCache(String dictCode) {
        if (StringUtils.isBlank(dictCode)) {
            return;
        }
        String cacheKey = StrUtil.format(RedisConstant.System.DICT_OPTIONS, dictCode);
        String versionKey = StrUtil.format(RedisConstant.System.DICT_VERSION, dictCode);

        Boolean deleted = redisTemplate.delete(cacheKey);
        Long newVersion = redisTemplate.opsForValue().increment(versionKey);
        // 给 version 设个 30 天过期,避免冷门字典的版本号无限累积
        redisTemplate.expire(versionKey, Duration.ofDays(30));
        log.info("[dict-cache] REFRESH dictCode={}, deleted={}, newVersion={}", dictCode, deleted, newVersion);
    }

    /**
     * 清空所有字典项缓存与版本号（慎用）。
     */
    @Override
    public void refreshAllDictCache() {
        Set<String> optionKeys = redisTemplate.keys(
                StrUtil.format(RedisConstant.System.DICT_OPTIONS, "*"));
        Set<String> versionKeys = redisTemplate.keys(
                StrUtil.format(RedisConstant.System.DICT_VERSION, "*"));

        int optCount = optionKeys == null ? 0 : optionKeys.size();
        int verCount = versionKeys == null ? 0 : versionKeys.size();
        if (optCount > 0) {
            redisTemplate.delete(optionKeys);
        }
        if (verCount > 0) {
            // 全量刷场景下,把 version 也清掉,下次读会重新初始化
            redisTemplate.delete(versionKeys);
        }
        log.warn("[dict-cache] CLEAR_ALL, options={} 个, version={} 个", optCount, verCount);
    }

    /** 根据Id修改状态 */
    @Override
    public Boolean updateDictItemStatus(Long id, Boolean status) {
        // 先查 dictCode,写完再刷新缓存(因为 updateById 不返回 dictCode)
        SysDictItem before = this.getById(id);
        LambdaUpdateWrapper<SysDictItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysDictItem::getId, id)
                .set(SysDictItem::getStatus, status ? 1 : 0);
        boolean ok = this.update(updateWrapper);
        if (ok && before != null) {
            refreshDictCache(before.getDictCode());
        }
        return ok;
    }

    /** 保存字典数据(分类属性) */
    @Override
    public boolean saveDictItem(SysDictItemForm formData) {
        SysDictItem entity = sysDictItemConverter.toEntity(formData);
        boolean ok = this.save(entity);
        if (ok) {
            refreshDictCache(formData.getDictCode());
        }
        return ok;
    }

    /** 获取字典数据表单 */
    @Override
    public SysDictItemForm getDictItemForm(Long itemId) {
        SysDictItem entity = this.getById(itemId);
        return sysDictItemConverter.toForm(entity);
    }

    /** 更新字典数据 */
    @Override
    public boolean updateDictItem(SysDictItemForm formData) {
        SysDictItem entity = sysDictItemConverter.toEntity(formData);
        boolean ok = this.updateById(entity);
        if (ok) {
            refreshDictCache(formData.getDictCode());
        }
        return ok;
    }

    /** 批量删除字典数据 */
    @Override
    public boolean deleteByIds(String ids) {
        if (StringUtils.isNotBlank(ids)) {
            List<String> itemIds = Arrays.stream(ids.split(",")).toList();
            // 先查出所有被删项的 dictCode,用于精准刷新
            List<SysDictItem> before = this.listByIds(itemIds);
            int delete = sysDictItemMapper.deleteByIds(itemIds);
            if (delete > 0) {
                before.stream()
                        .map(SysDictItem::getDictCode)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .forEach(this::refreshDictCache);
            }
            return delete > 0;
        }
        return false;
    }
}