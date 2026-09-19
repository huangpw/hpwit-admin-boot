package com.nova.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nova.common.constant.RedisConstant;
import com.nova.system.converter.SysConfigConverter;
import com.nova.system.mapper.SysConfigMapper;
import com.nova.system.model.entity.SysConfig;
import com.nova.system.model.form.SysConfigForm;
import com.nova.system.model.query.SysConfigPageQuery;
import com.nova.system.model.vo.SysConfigVo;
import com.nova.system.service.SysConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置Service接口实现
 *
 * @author Theo
 * @since 2024-07-29 11:17:26
 */
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private final SysConfigConverter configConverter;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 系统启动完成后，加载系统配置到缓存
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 分页查询系统配置
     *
     * @param configPageQuery 查询参数
     * @return 系统配置分页列表
     */
    @Override
    public IPage<SysConfigVo> page(SysConfigPageQuery configPageQuery) {
        Page<SysConfig> page = new Page<>(configPageQuery.getPageNum(), configPageQuery.getPageSize());
        String keywords = configPageQuery.getKeywords();
        LambdaQueryWrapper<SysConfig> query = new LambdaQueryWrapper<SysConfig>()
                .and(StringUtils.isNotBlank(keywords),
                    q -> q.like(SysConfig::getConfigKey, keywords)
                        .or()
                        .like(SysConfig::getConfigName, keywords)
                );
        Page<SysConfig> pageList = this.page(page, query);
        return configConverter.toPageVo(pageList);
    }

    /**
     * 保存系统配置
     *
     * @param configForm 系统配置表单
     * @return 是否保存成功
     */
    @Override
    public boolean save(SysConfigForm configForm) {
        Assert.isTrue(
                super.count(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configForm.getConfigKey())) == 0,
                "配置键已存在");
        SysConfig config = configConverter.toEntity(configForm);
        config.setCreateBy(StpUtil.getLoginIdAsLong());
        config.setIsDeleted(0);
        return this.save(config);
    }

    /**
     * 获取系统配置表单数据
     *
     * @param id 系统配置ID
     * @return 系统配置表单数据
     */
    @Override
    public SysConfigForm getConfigFormData(Long id) {
        SysConfig entity = this.getById(id);
        return configConverter.toForm(entity);
    }

    /**
     * 编辑系统配置
     *
     * @param id         系统配置ID
     * @param configForm 系统配置表单
     * @return 是否编辑成功
     */
    @Override
    public boolean edit(Long id, SysConfigForm configForm) {
        Assert.isTrue(
                super.count(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configForm.getConfigKey()).ne(SysConfig::getId, id)) == 0,
                "配置键已存在");
        SysConfig config = configConverter.toEntity(configForm);
        config.setUpdateBy(StpUtil.getLoginIdAsLong());
        return this.updateById(config);
    }

    /**
     * 删除系统配置
     *
     * @param id 系统配置ID
     * @return 是否删除成功
     */
    @Override
    public boolean delete(Long id) {
        if (id != null) {
            return super.update(new LambdaUpdateWrapper<SysConfig>()
                    .eq(SysConfig::getId,id)
                    .set(SysConfig::getIsDeleted, 1)
                    .set(SysConfig::getUpdateBy, StpUtil.getLoginIdAsLong())
            );
        }
        return false;
    }

    /**
     * 刷新系统配置缓存
     *
     * @return 是否刷新成功
     */
    @Override
    public boolean refreshCache() {
        redisTemplate.delete(RedisConstant.System.CONFIG);
        List<SysConfig> list = this.list();
        if (list != null) {
            Map<String, String> map = list.stream().collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
            redisTemplate.opsForHash().putAll(RedisConstant.System.CONFIG, map);
            return true;
        }
        return false;
    }

    /**
     * 获取系统配置
     *
     * @param key 配置键
     * @return 配置值
     */
    @Override
    public Object getSystemConfig(String key) {
        if (StringUtils.isNotBlank(key)) {
            return redisTemplate.opsForHash().get(RedisConstant.System.CONFIG, key);
        }
        return null;
    }

}
