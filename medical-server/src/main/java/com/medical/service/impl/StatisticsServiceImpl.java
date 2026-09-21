package com.medical.service.impl;

import com.medical.mapper.StatisticsMapper;
import com.medical.service.StatisticsService;
import com.medical.vo.NameValueVO;
import com.medical.vo.StatisticsOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatisticsMapper statisticsMapper;

    @Override
    public StatisticsOverviewVO overview() {
        StatisticsOverviewVO vo = new StatisticsOverviewVO();
        vo.setTotalAppointments(statisticsMapper.countAllAppointments());
        vo.setTodayAppointments(statisticsMapper.countTodayAppointments());
        vo.setPendingAppointments(statisticsMapper.countAppointmentsByStatus(0));
        vo.setCompletedAppointments(statisticsMapper.countAppointmentsByStatus(1));
        vo.setCancelledAppointments(statisticsMapper.countAppointmentsByStatus(2));
        vo.setTotalDoctors(statisticsMapper.countDoctors());
        vo.setTotalPatients(statisticsMapper.countPatients());
        vo.setTotalDepartments(statisticsMapper.countDepartments());
        vo.setTotalRecords(statisticsMapper.countRecords());
        vo.setTotalPrescriptions(statisticsMapper.countPrescriptions());
        vo.setTotalPrescriptionAmount(statisticsMapper.sumPrescriptionAmount());
        return vo;
    }

    @Override
    public List<NameValueVO> departmentRanking() {
        return statisticsMapper.countByDepartment();
    }

    @Override
    public List<NameValueVO> doctorRanking() {
        return statisticsMapper.countByDoctor();
    }

    /**
     * 未来 7 天就诊预约分布。数据库只返回有数据的日期，这里补齐缺失日期为 0，
     * 避免前端柱状图出现断点。
     */
    @Override
    public List<NameValueVO> upcomingTrend() {
        Map<String, Long> actual = statisticsMapper.countUpcomingTrend().stream()
                .collect(Collectors.toMap(NameValueVO::getName, NameValueVO::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        List<NameValueVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            String day = today.plusDays(i).format(DATE_FMT);
            result.add(new NameValueVO(day, actual.getOrDefault(day, 0L)));
        }
        return result;
    }

    @Override
    public List<NameValueVO> timeSlotDistribution() {
        return statisticsMapper.countByTimeSlot();
    }
}
