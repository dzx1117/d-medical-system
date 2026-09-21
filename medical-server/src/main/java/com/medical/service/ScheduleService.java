package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.entity.Schedule;
import com.medical.vo.ScheduleVO;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    Page<ScheduleVO> page(Integer pageNum, Integer pageSize, Long doctorId, Long departmentId, LocalDate scheduleDate);

    /**
     * 患者端查询可预约号源（仅返回未来日期、有剩余号源的排班）
     */
    List<ScheduleVO> listAvailable(Long departmentId, Long doctorId, LocalDate scheduleDate);

    ScheduleVO getVOById(Long id);

    void save(Schedule schedule);

    void update(Schedule schedule);

    void delete(Long id);
}
