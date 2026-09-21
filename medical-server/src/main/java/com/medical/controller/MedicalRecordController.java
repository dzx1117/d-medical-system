package com.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.Result;
import com.medical.common.SecurityUtil;
import com.medical.dto.MedicalRecordDTO;
import com.medical.entity.Doctor;
import com.medical.mapper.DoctorMapper;
import com.medical.service.MedicalRecordService;
import com.medical.vo.MedicalRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/record")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final DoctorMapper doctorMapper;

    /**
     * 患者端：我的病历列表
     */
    @GetMapping("/my")
    public Result<Page<MedicalRecordVO>> myRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Long patientId = SecurityUtil.getCurrentUserId();
        return Result.success(medicalRecordService.pagePatientRecords(patientId, pageNum, pageSize));
    }

    /**
     * 医生端：我填写的病历列表
     */
    @GetMapping("/doctor")
    public Result<Page<MedicalRecordVO>> doctorRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Long doctorId = getCurrentDoctorId();
        return Result.success(medicalRecordService.pageDoctorRecords(doctorId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<MedicalRecordVO> getById(@PathVariable Long id) {
        return Result.success(medicalRecordService.getVOById(id));
    }

    /**
     * 根据预约ID查病历，不存在时返回 null（前端据此显示"填写病历"按钮）
     */
    @GetMapping("/by-appointment/{appointmentId}")
    public Result<MedicalRecordVO> getByAppointment(@PathVariable Long appointmentId) {
        return Result.success(medicalRecordService.getByAppointmentId(appointmentId));
    }

    /**
     * 医生填写病历
     */
    @PostMapping
    public Result<MedicalRecordVO> save(@Valid @RequestBody MedicalRecordDTO dto) {
        Long doctorId = getCurrentDoctorId();
        return Result.success("病历保存成功", medicalRecordService.saveRecord(doctorId, dto));
    }

    /**
     * 医生修改病历
     */
    @PutMapping
    public Result<MedicalRecordVO> update(@Valid @RequestBody MedicalRecordDTO dto) {
        Long doctorId = getCurrentDoctorId();
        return Result.success("病历修改成功", medicalRecordService.updateRecord(doctorId, dto));
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
