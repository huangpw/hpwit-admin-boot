package com.hpwit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hpwit.system.model.entity.SysUserNotice;
import com.hpwit.system.model.query.SysNoticePageQuery;
import com.hpwit.system.model.vo.SysNoticePageVo;
import com.hpwit.system.model.vo.SysUserNoticePageVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户公告状态Mapper接口
 *
 * @author youlaitech
 * @since 2024-08-28 16:56
 */
@Mapper
public interface SysUserNoticeMapper extends BaseMapper<SysUserNotice> {
    /**
     * 分页获取我的通知公告
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return 通知公告分页列表
     */
    IPage<SysUserNoticePageVo> getMyNoticePage(Page<SysNoticePageVo> page, @Param("queryParams") SysNoticePageQuery queryParams);
}
