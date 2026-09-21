package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.dto.PrescriptionDTO;
import com.medical.vo.PrescriptionVO;

public interface PrescriptionService {

    /**
     * 医生开处方，同时扣减药品库存
     */
    PrescriptionVO createPrescription(Long doctorId, PrescriptionDTO dto);

    /**
     * 患者端：我的处方
     */
    Page<PrescriptionVO> pagePatientPrescriptions(Long patientId, Integer pageNum, Integer pageSize);

    /**
     * 医生端：我开的处方
     */
    Page<PrescriptionVO> pageDoctorPrescriptions(Long doctorId, Integer pageNum, Integer pageSize);

    PrescriptionVO getVOById(Long id);

    /**
     * 患者缴费，处方状态改为已缴费
     */
    void payPrescription(Long patientId, Long prescriptionId);

    /**
     * 患者取药，处方状态改为已取药
     */
    void pickupPrescription(Long patientId, Long prescriptionId);
}
