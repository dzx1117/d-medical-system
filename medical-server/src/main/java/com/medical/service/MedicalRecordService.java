package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.dto.MedicalRecordDTO;
import com.medical.vo.MedicalRecordVO;

public interface MedicalRecordService {

    /**
     * 医生填写病历
     */
    MedicalRecordVO saveRecord(Long doctorId, MedicalRecordDTO dto);

    /**
     * 医生修改病历
     */
    MedicalRecordVO updateRecord(Long doctorId, MedicalRecordDTO dto);

    /**
     * 患者端：我的病历
     */
    Page<MedicalRecordVO> pagePatientRecords(Long patientId, Integer pageNum, Integer pageSize);

    /**
     * 医生端：我写的病历
     */
    Page<MedicalRecordVO> pageDoctorRecords(Long doctorId, Integer pageNum, Integer pageSize);

    MedicalRecordVO getVOById(Long id);

    /**
     * 根据预约ID查询病历（用于就诊详情页）
     */
    MedicalRecordVO getByAppointmentId(Long appointmentId);
}
