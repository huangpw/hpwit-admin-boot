package com.hpwit.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hpwit.system.model.entity.SysLog;
import com.hpwit.system.model.query.SysLogPageQuery;
import com.hpwit.system.model.vo.SysLogPageVo;

/**
 * 系统日志 服务接口
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
public interface SysLogService extends IService<SysLog> {

    /**
     * 获取日志分页列表
     */
    Page<SysLogPageVo> getLogPage(SysLogPageQuery queryParams);


    /**
     * 获取访问趋势
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     */
    //VisitTrendVO getVisitTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取访问统计
     */
    //VisitStatsVO getVisitStats();

}
