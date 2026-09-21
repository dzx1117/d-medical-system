package com.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.Result;
import com.medical.common.SecurityUtil;
import com.medical.dto.PrescriptionDTO;
import com.medical.entity.Doctor;
import com.medical.mapper.DoctorMapper;
import com.medical.service.PrescriptionService;
import com.medical.vo.PrescriptionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prescription")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final DoctorMapper doctorMapper;

    /**
     * 医生开处方
     */
    @PostMapping
    public Result<PrescriptionVO> create(@Valid @RequestBody PrescriptionDTO dto) {
        Long doctorId = getCurrentDoctorId();
        return Result.success("开方成功", prescriptionService.createPrescription(doctorId, dto));
    }

    /**
     * 患者端：我的处方
     */
    @GetMapping("/my")
    public Result<Page<PrescriptionVO>> myPrescriptions(@RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        Long patientId = SecurityUtil.getCurrentUserId();
        return Result.success(prescriptionService.pagePatientPrescriptions(patientId, pageNum, pageSize));
    }

    /**
     * 医生端：我开的处方
     */
    @GetMapping("/doctor")
    public Result<Page<PrescriptionVO>> doctorPrescriptions(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long doctorId = getCurrentDoctorId();
        return Result.success(prescriptionService.pageDoctorPrescriptions(doctorId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<PrescriptionVO> getById(@PathVariable Long id) {
        return Result.success(prescriptionService.getVOById(id));
    }

    /**
     * 患者缴费
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        Long patientId = SecurityUtil.getCurrentUserId();
        prescriptionService.payPrescription(patientId, id);
        return Result.successMsg("缴费成功");
    }

    /**
     * 患者取药
     */
    @PostMapping("/pickup/{id}")
    public Result<Void> pickup(@PathVariable Long id) {
        Long patientId = SecurityUtil.getCurrentUserId();
        prescriptionService.pickupPrescription(patientId, id);
        return Result.successMsg("取药成功");
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
