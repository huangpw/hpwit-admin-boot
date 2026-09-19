package com.nova.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nova.system.mapper.SysUserNoticeMapper;
import com.nova.system.model.entity.SysUserNotice;
import com.nova.system.model.query.SysNoticePageQuery;
import com.nova.system.model.vo.SysNoticePageVo;
import com.nova.system.model.vo.SysUserNoticePageVo;
import com.nova.system.service.SysUserNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户公告状态服务实现类
 *
 * @author huangpw
 * @since 2024-08-28 16:56
 */
@Service
@RequiredArgsConstructor
public class SysUserNoticeServiceImpl extends ServiceImpl<SysUserNoticeMapper, SysUserNotice> implements SysUserNoticeService {

    private final SysUserNoticeMapper sysUserNoticeMapper;

    /**
     * 全部标记为已读
     *
     * @return 是否成功
     */
    @Override
    public boolean readAll() {
        Long userId = StpUtil.getLoginIdAsLong();
        return this.update(new LambdaUpdateWrapper<SysUserNotice>()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsRead, 0)
                .set(SysUserNotice::getIsRead, 1)
        );
    }

    /**
     * 我的通知公告分页列表
     *
     * @param page        分页对象
     * @param queryParams 查询参数
     * @return 通知公告分页列表
     */
    @Override
    public IPage<SysUserNoticePageVo> getMyNoticePage(Page<SysNoticePageVo> page, SysNoticePageQuery queryParams) {
        return this.getBaseMapper().getMyNoticePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }


}
