package com.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.Result;
import com.medical.common.SecurityUtil;
import com.medical.entity.Doctor;
import com.medical.mapper.DoctorMapper;
import com.medical.service.AppointmentService;
import com.medical.vo.AppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final DoctorMapper doctorMapper;

    /**
     * 患者预约挂号
     */
    @PostMapping("/create")
    public Result<AppointmentVO> create(@RequestParam Long scheduleId) {
        Long patientId = SecurityUtil.getCurrentUserId();
        return Result.success("预约成功", appointmentService.createAppointment(patientId, scheduleId));
    }

    /**
     * 患者取消预约
     */
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long patientId = SecurityUtil.getCurrentUserId();
        appointmentService.cancelAppointment(patientId, id);
        return Result.successMsg("取消成功");
    }

    /**
     * 患者端：我的预约列表
     */
    @GetMapping("/my")
    public Result<Page<AppointmentVO>> myAppointments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) Integer status) {
        Long patientId = SecurityUtil.getCurrentUserId();
        return Result.success(appointmentService.pageMyAppointments(patientId, pageNum, pageSize, status));
    }

    /**
     * 医生端：我的患者预约列表
     */
    @GetMapping("/doctor")
    public Result<Page<AppointmentVO>> doctorAppointments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long doctorId = getCurrentDoctorId();
        return Result.success(appointmentService.pageDoctorAppointments(doctorId, pageNum, pageSize, status, date));
    }

    /**
     * 医生端：确认就诊完成
     */
    @PostMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id) {
        Long doctorId = getCurrentDoctorId();
        appointmentService.completeAppointment(doctorId, id);
        return Result.successMsg("已确认就诊");
    }

    @GetMapping("/{id}")
    public Result<AppointmentVO> getById(@PathVariable Long id) {
        return Result.success(appointmentService.getVOById(id));
    }

    private Long getCurrentDoctorId() {
        Long userId = SecurityUtil.getCurrentUserId();
        Doctor doctor = doctorMapper.selectOne(new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, userId));
        if (doctor == null) {
            throw new BusinessException("当前账号不是医生账号");
        }
        return doctor.getId();
    }
}
