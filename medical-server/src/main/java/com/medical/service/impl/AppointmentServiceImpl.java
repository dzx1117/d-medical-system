package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.RedisLock;
import com.medical.entity.*;
import com.medical.mapper.*;
import com.medical.service.AppointmentService;
import com.medical.vo.AppointmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    /** 单个号源的锁键前缀 */
    private static final String LOCK_KEY_PREFIX = "lock:schedule:";
    /** 最长等待加锁时间 */
    private static final long LOCK_WAIT_MS = 3000L;
    /** 锁自动过期时间，防止服务宕机后死锁 */
    private static final long LOCK_EXPIRE_MS = 10000L;
    /** 自旋重试间隔 */
    private static final long LOCK_RETRY_INTERVAL_MS = 50L;

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final RedisLock redisLock;
    private final AppointmentTxService appointmentTxService;

    @Override
    public AppointmentVO createAppointment(Long patientId, Long scheduleId) {
        String lockKey = LOCK_KEY_PREFIX + scheduleId;

        // Redis 分布式锁把同一号源的并发请求串行化；
        // 事务在 appointmentTxService 中提交后才返回，之后才释放锁。
        Appointment appointment = redisLock.executeWithLock(
                lockKey, LOCK_WAIT_MS, LOCK_EXPIRE_MS, LOCK_RETRY_INTERVAL_MS,
                () -> appointmentTxService.doCreateAppointment(patientId, scheduleId));

        return convertList(Collections.singletonList(appointment)).get(0);
    }

    @Override
    public void cancelAppointment(Long patientId, Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessException("无权操作他人的预约记录");
        }
        if (appointment.getStatus() != 0) {
            throw new BusinessException("当前状态无法取消");
        }
        appointmentTxService.doCancelAppointment(appointment);
    }

    @Override
    public Page<AppointmentVO> pageMyAppointments(Long patientId, Integer pageNum,
                                                  Integer pageSize, Integer status) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getPatientId, patientId)
               .eq(status != null, Appointment::getStatus, status)
               .orderByDesc(Appointment::getCreateTime);
        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(page);
    }

    @Override
    public Page<AppointmentVO> pageDoctorAppointments(Long doctorId, Integer pageNum, Integer pageSize,
                                                      Integer status, LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
               .eq(status != null, Appointment::getStatus, status)
               .eq(date != null, Appointment::getAppointmentDate, date)
               .orderByDesc(Appointment::getCreateTime);
        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(page);
    }

    @Override
    public AppointmentVO getVOById(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        return convertList(Collections.singletonList(appointment)).get(0);
    }

    @Override
    public void completeAppointment(Long doctorId, Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作他人的预约记录");
        }
        if (appointment.getStatus() != 0) {
            throw new BusinessException("当前状态无法完成就诊");
        }
        appointmentTxService.doCompleteAppointment(appointment);
    }

    private Page<AppointmentVO> convertPage(Page<Appointment> page) {
        Page<AppointmentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(convertList(page.getRecords()));
        return voPage;
    }

    private List<AppointmentVO> convertList(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> patientIds = appointments.stream().map(Appointment::getPatientId).distinct().collect(Collectors.toList());
        List<Long> doctorIds = appointments.stream().map(Appointment::getDoctorId).distinct().collect(Collectors.toList());

        Map<Long, User> patientMap = userMapper.selectBatchIds(patientIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Doctor> doctors = doctorMapper.selectBatchIds(doctorIds);
        Map<Long, Doctor> doctorMap = doctors.stream().collect(Collectors.toMap(Doctor::getId, Function.identity()));

        List<Long> doctorUserIds = doctors.stream().map(Doctor::getUserId).collect(Collectors.toList());
        Map<Long, User> doctorUserMap = doctorUserIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(doctorUserIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> deptIds = doctors.stream().map(Doctor::getDepartmentId).distinct().collect(Collectors.toList());
        Map<Long, Department> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(Department::getId, Function.identity()));

        return appointments.stream().map(appointment -> {
            AppointmentVO vo = new AppointmentVO();
            vo.setId(appointment.getId());
            vo.setAppointmentNo(appointment.getAppointmentNo());
            vo.setPatientId(appointment.getPatientId());
            vo.setDoctorId(appointment.getDoctorId());
            vo.setAppointmentDate(appointment.getAppointmentDate());
            vo.setTimeSlot(appointment.getTimeSlot());
            vo.setTimeSlotDesc(timeSlotDesc(appointment.getTimeSlot()));
            vo.setFee(appointment.getFee());
            vo.setStatus(appointment.getStatus());
            vo.setStatusDesc(statusDesc(appointment.getStatus()));
            vo.setVisitTime(appointment.getVisitTime());
            vo.setCreateTime(appointment.getCreateTime());

            User patient = patientMap.get(appointment.getPatientId());
            if (patient != null) {
                vo.setPatientName(maskName(patient.getRealName()));
            }

            Doctor doctor = doctorMap.get(appointment.getDoctorId());
            if (doctor != null) {
                vo.setTitle(doctor.getTitle());
                User doctorUser = doctorUserMap.get(doctor.getUserId());
                if (doctorUser != null) {
                    vo.setDoctorName(doctorUser.getRealName());
                }
                Department department = deptMap.get(doctor.getDepartmentId());
                if (department != null) {
                    vo.setDepartmentName(department.getDeptName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /** 患者姓名脱敏：张三 → 张*，欧阳修 → 欧** */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private String timeSlotDesc(Integer timeSlot) {
        if (timeSlot == null) {
            return "";
        }
        return switch (timeSlot) {
            case 1 -> "上午";
            case 2 -> "下午";
            case 3 -> "晚上";
            default -> "未知";
        };
    }

    private String statusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待就诊";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }
}
