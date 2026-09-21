package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.BusinessException;
import com.medical.entity.Appointment;
import com.medical.entity.Schedule;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预约创建的「事务边界」。
 *
 * <p>独立成 Bean 是为了让 Spring 事务代理生效：调用方 {@code AppointmentServiceImpl}
 * 在持有 Redis 分布式锁的前提下调用本类，事务提交后才返回，之后才释放锁，
 * 从而避免「锁已释放但事务未提交」导致的重复预约窗口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentTxService {

    private final AppointmentMapper appointmentMapper;
    private final ScheduleMapper scheduleMapper;

    @Transactional(rollbackFor = Exception.class)
    public Appointment doCreateAppointment(Long patientId, Long scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("号源不存在");
        }
        if (schedule.getStatus() != 1) {
            throw new BusinessException("该号源已停诊");
        }
        if (schedule.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约已过期的号源");
        }

        // 防止同一患者重复预约同一号源
        LambdaQueryWrapper<Appointment> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(Appointment::getPatientId, patientId)
                  .eq(Appointment::getScheduleId, scheduleId)
                  .eq(Appointment::getStatus, 0);
        if (appointmentMapper.selectOne(dupWrapper) != null) {
            throw new BusinessException("您已预约该号源，请勿重复预约");
        }

        // 数据库原子更新作为最后一道防线：仅当 booked_slots < total_slots 时才 +1
        int affected = scheduleMapper.incrementBookedSlots(scheduleId);
        if (affected == 0) {
            throw new BusinessException("该号源已被约满，请选择其他号源");
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(generateAppointmentNo());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(schedule.getScheduleDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setFee(schedule.getFee());
        appointment.setStatus(0);
        appointmentMapper.insert(appointment);

        log.info("预约成功: patientId={}, scheduleId={}, appointmentNo={}",
                patientId, scheduleId, appointment.getAppointmentNo());
        return appointment;
    }

    @Transactional(rollbackFor = Exception.class)
    public void doCancelAppointment(Appointment appointment) {
        appointment.setStatus(2);
        appointment.setCancelTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
        scheduleMapper.decrementBookedSlots(appointment.getScheduleId());
        log.info("取消预约成功: appointmentId={}", appointment.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void doCompleteAppointment(Appointment appointment) {
        appointment.setStatus(1);
        appointment.setVisitTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    /** 预约单号：yyyyMMddHHmmss + 6 位随机数 */
    private String generateAppointmentNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return timestamp + random;
    }
}
