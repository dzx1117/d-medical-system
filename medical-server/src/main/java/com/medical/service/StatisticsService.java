package com.medical.service;

import com.medical.vo.NameValueVO;
import com.medical.vo.StatisticsOverviewVO;

import java.util.List;

public interface StatisticsService {

    StatisticsOverviewVO overview();

    List<NameValueVO> departmentRanking();

    List<NameValueVO> doctorRanking();

    List<NameValueVO> upcomingTrend();

    List<NameValueVO> timeSlotDistribution();
}
