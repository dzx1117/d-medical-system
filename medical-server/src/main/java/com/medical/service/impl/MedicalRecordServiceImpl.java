package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.dto.MedicalRecordDTO;
import com.medical.entity.Appointment;
import com.medical.entity.Department;
import com.medical.entity.Doctor;
import com.medical.entity.MedicalRecord;
import com.medical.entity.User;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DepartmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.MedicalRecordMapper;
import com.medical.mapper.UserMapper;
import com.medical.service.MedicalRecordService;
import com.medical.vo.MedicalRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecordVO saveRecord(Long doctorId, MedicalRecordDTO dto) {
        Appointment appointment = appointmentMapper.selectById(dto.getAppointmentId());
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("只能为自己的患者填写病历");
        }
        if (appointment.getStatus() == 2) {
            throw new BusinessException("该预约已取消，无法填写病历");
        }

        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getAppointmentId, dto.getAppointmentId());
        if (medicalRecordMapper.selectOne(wrapper) != null) {
            throw new BusinessException("该预约已存在病历，请使用修改功能");
        }

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(appointment.getPatientId());
        record.setDoctorId(doctorId);
        record.setAppointmentId(dto.getAppointmentId());
        record.setVisitDate(dto.getVisitDate() == null ? LocalDate.now() : dto.getVisitDate());
        record.setChiefComplaint(dto.getChiefComplaint());
        record.setPresentIllness(dto.getPresentIllness());
        record.setDiagnosis(dto.getDiagnosis());
        record.setTreatmentPlan(dto.getTreatmentPlan());
        record.setRemark(dto.getRemark());
        medicalRecordMapper.insert(record);

        return convertList(Collections.singletonList(record)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecordVO updateRecord(Long doctorId, MedicalRecordDTO dto) {
        MedicalRecord record = medicalRecordMapper.selectById(dto.getId());
        if (record == null) {
            throw new BusinessException("病历不存在");
        }
        if (!record.getDoctorId().equals(doctorId)) {
            throw new BusinessException("只能修改自己填写的病历");
        }

        record.setChiefComplaint(dto.getChiefComplaint());
        record.setPresentIllness(dto.getPresentIllness());
        record.setDiagnosis(dto.getDiagnosis());
        record.setTreatmentPlan(dto.getTreatmentPlan());
        record.setRemark(dto.getRemark());
        if (dto.getVisitDate() != null) {
            record.setVisitDate(dto.getVisitDate());
        }
        medicalRecordMapper.updateById(record);

        return convertList(Collections.singletonList(record)).get(0);
    }

    @Override
    public Page<MedicalRecordVO> pagePatientRecords(Long patientId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getPatientId, patientId)
               .orderByDesc(MedicalRecord::getVisitDate, MedicalRecord::getCreateTime);
        Page<MedicalRecord> page = medicalRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(page);
    }

    @Override
    public Page<MedicalRecordVO> pageDoctorRecords(Long doctorId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getDoctorId, doctorId)
               .orderByDesc(MedicalRecord::getVisitDate, MedicalRecord::getCreateTime);
        Page<MedicalRecord> page = medicalRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(page);
    }

    @Override
    public MedicalRecordVO getVOById(Long id) {
        MedicalRecord record = medicalRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("病历不存在");
        }
        return convertList(Collections.singletonList(record)).get(0);
    }

    @Override
    public MedicalRecordVO getByAppointmentId(Long appointmentId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getAppointmentId, appointmentId);
        MedicalRecord record = medicalRecordMapper.selectOne(wrapper);
        if (record == null) {
            return null;
        }
        return convertList(Collections.singletonList(record)).get(0);
    }

    private Page<MedicalRecordVO> convertPage(Page<MedicalRecord> page) {
        Page<MedicalRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(convertList(page.getRecords()));
        return voPage;
    }

    private List<MedicalRecordVO> convertList(List<MedicalRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> patientIds = records.stream().map(MedicalRecord::getPatientId).distinct().collect(Collectors.toList());
        List<Long> doctorIds = records.stream().map(MedicalRecord::getDoctorId).distinct().collect(Collectors.toList());

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

        return records.stream().map(record -> {
            MedicalRecordVO vo = new MedicalRecordVO();
            vo.setId(record.getId());
            vo.setPatientId(record.getPatientId());
            vo.setDoctorId(record.getDoctorId());
            vo.setAppointmentId(record.getAppointmentId());
            vo.setVisitDate(record.getVisitDate());
            vo.setChiefComplaint(record.getChiefComplaint());
            vo.setPresentIllness(record.getPresentIllness());
            vo.setDiagnosis(record.getDiagnosis());
            vo.setTreatmentPlan(record.getTreatmentPlan());
            vo.setRemark(record.getRemark());
            vo.setCreateTime(record.getCreateTime());

            User patient = patientMap.get(record.getPatientId());
            if (patient != null) {
                // 医疗数据脱敏：患者姓名仅保留姓氏
                vo.setPatientName(maskName(patient.getRealName()));
            }

            Doctor doctor = doctorMap.get(record.getDoctorId());
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

    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
