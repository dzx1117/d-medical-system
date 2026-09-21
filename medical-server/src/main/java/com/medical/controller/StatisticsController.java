package com.medical.controller;

import com.medical.common.Result;
import com.medical.service.StatisticsService;
import com.medical.vo.NameValueVO;
import com.medical.vo.StatisticsOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理后台统计看板。整个路径在 SecurityConfig 中限定为管理员角色。
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<StatisticsOverviewVO> overview() {
        return Result.success(statisticsService.overview());
    }

    @GetMapping("/department-ranking")
    public Result<List<NameValueVO>> departmentRanking() {
        return Result.success(statisticsService.departmentRanking());
    }

    @GetMapping("/doctor-ranking")
    public Result<List<NameValueVO>> doctorRanking() {
        return Result.success(statisticsService.doctorRanking());
    }

    @GetMapping("/upcoming-trend")
    public Result<List<NameValueVO>> upcomingTrend() {
        return Result.success(statisticsService.upcomingTrend());
    }

    @GetMapping("/time-slot")
    public Result<List<NameValueVO>> timeSlotDistribution() {
        return Result.success(statisticsService.timeSlotDistribution());
    }
}
