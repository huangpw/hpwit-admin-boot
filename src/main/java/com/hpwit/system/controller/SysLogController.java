package com.hpwit.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hpwit.common.result.PageResult;
import com.hpwit.system.model.query.SysLogPageQuery;
import com.hpwit.system.model.vo.SysLogPageVo;
import com.hpwit.system.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志控制层
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Tag(name = "10.日志接口")
@RestController
@RequestMapping("/v1/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @Operation(summary = "日志分页列表")
    @GetMapping("/page")
    public PageResult<SysLogPageVo> getLogPage(SysLogPageQuery queryParams) {
        Page<SysLogPageVo> result = sysLogService.getLogPage(queryParams);
        return PageResult.success(result);
    }

    //@Operation(summary = "获取访问趋势")
    //@GetMapping("/visit-trend")
    //public Result<VisitTrendVO> getVisitTrend(
    //        @Parameter(description = "开始时间", example = "yyyy-MM-dd") @RequestParam String startDate,
    //        @Parameter(description = "结束时间", example = "yyyy-MM-dd") @RequestParam String endDate
    //) {
    //    LocalDate start = LocalDate.parse(startDate);
    //    LocalDate end = LocalDate.parse(endDate);
    //    VisitTrendVO data = logService.getVisitTrend(start, end);
    //    return Result.success(data);
    //}

    //@Operation(summary = "获取访问统计")
    //@GetMapping("/visit-stats")
    //public Result<VisitStatsVO> getVisitStats() {
    //    VisitStatsVO result = logService.getVisitStats();
    //    return Result.success(result);
    //}

}
