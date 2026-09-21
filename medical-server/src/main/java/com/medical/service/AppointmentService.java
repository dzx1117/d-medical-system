package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.vo.AppointmentVO;

public interface AppointmentService {

    /**
     * 患者预约挂号。使用 Redis 分布式锁 + 数据库原子更新双重保证不超卖。
     */
    AppointmentVO createAppointment(Long patientId, Long scheduleId);

    /**
     * 患者取消预约，释放号源。
     */
    void cancelAppointment(Long patientId, Long appointmentId);

    /**
     * 患者端：查询我的预约
     */
    Page<AppointmentVO> pageMyAppointments(Long patientId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 医生端：查询我的患者预约
     */
    Page<AppointmentVO> pageDoctorAppointments(Long doctorId, Integer pageNum, Integer pageSize,
                                               Integer status, java.time.LocalDate date);

    AppointmentVO getVOById(Long id);

    /**
     * 医生确认就诊，预约状态改为已完成
     */
    void completeAppointment(Long doctorId, Long appointmentId);
}
