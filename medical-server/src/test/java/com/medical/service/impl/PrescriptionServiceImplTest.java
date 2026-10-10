package com.medical.service.impl;

import com.medical.common.BusinessException;
import com.medical.entity.MedicalRecord;
import com.medical.entity.Prescription;
import com.medical.entity.User;
import com.medical.mapper.DepartmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.MedicalRecordMapper;
import com.medical.mapper.MedicineMapper;
import com.medical.mapper.PrescriptionItemMapper;
import com.medical.mapper.PrescriptionMapper;
import com.medical.mapper.UserMapper;
import com.medical.vo.PrescriptionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PrescriptionServiceImpl} 的单元测试。
 *
 * <p>重点是缴费 / 取药这两个写操作：状态判断和状态更新都在数据库的一条 SQL 里，
 * 所以「影响行数为 0」是唯一可靠的失败信号，不能退化成先查后判。
 * 这几条用例就是在钉住这个约定 —— 一旦有人把它改成 {@code selectById + if}，测试会红。
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    private static final long PATIENT_ID = 100L;

    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    private MedicalRecordMapper medicalRecordMapper;
    @Mock
    private MedicineMapper medicineMapper;
    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    @Test
    @DisplayName("缴费成功：影响行数为 1 时正常返回")
    void paySucceedsWhenStatusTransitionApplies() {
        when(prescriptionMapper.selectById(1L)).thenReturn(prescriptionOf(1L, PATIENT_ID));
        when(prescriptionMapper.payIfPending(1L)).thenReturn(1);

        assertThatCode(() -> prescriptionService.payPrescription(PATIENT_ID, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("并发下抢输的一方：影响行数为 0，必须报错而不是默默成功")
    void payFailsWhenStatusTransitionAffectsNoRow() {
        when(prescriptionMapper.selectById(1L)).thenReturn(prescriptionOf(1L, PATIENT_ID));
        // 另一个请求已经把 status 从 0 改成 1，这条 UPDATE 的 WHERE 不再匹配
        when(prescriptionMapper.payIfPending(1L)).thenReturn(0);

        assertThatThrownBy(() -> prescriptionService.payPrescription(PATIENT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态无法缴费");
    }

    @Test
    @DisplayName("没缴费就取药：影响行数为 0，提示先缴费")
    void pickupFailsWhenNotPaid() {
        when(prescriptionMapper.selectById(1L)).thenReturn(prescriptionOf(1L, PATIENT_ID));
        when(prescriptionMapper.pickupIfPaid(1L)).thenReturn(0);

        assertThatThrownBy(() -> prescriptionService.pickupPrescription(PATIENT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先完成缴费");
    }

    @Test
    @DisplayName("操作别人的处方被拒，且不会走到状态更新")
    void cannotOperateOnSomeoneElsesPrescription() {
        when(prescriptionMapper.selectById(1L)).thenReturn(prescriptionOf(1L, 999L));

        assertThatThrownBy(() -> prescriptionService.payPrescription(PATIENT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作他人的处方");

        // 越权请求连 UPDATE 都不该发出去
        verify(prescriptionMapper, never()).payIfPending(anyLong());
    }

    @Test
    @DisplayName("处方不存在时报错，而不是抛空指针")
    void missingPrescriptionIsRejected() {
        when(prescriptionMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> prescriptionService.payPrescription(PATIENT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("处方不存在");
    }

    @Test
    @DisplayName("处方列表里的患者姓名做了脱敏，医生姓名保持完整")
    void viewObjectMasksPatientNameButNotDoctorName() {
        Prescription prescription = prescriptionOf(1L, PATIENT_ID);

        User patient = new User();
        patient.setId(PATIENT_ID);
        patient.setRealName("张三丰");

        when(prescriptionMapper.selectById(1L)).thenReturn(prescription);
        when(prescriptionItemMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(patient));
        when(doctorMapper.selectBatchIds(any())).thenReturn(List.of());
        when(medicalRecordMapper.selectBatchIds(any())).thenReturn(List.of(recordOf(300L)));

        PrescriptionVO vo = prescriptionService.getVOById(1L);

        assertThat(vo.getPatientName()).isEqualTo("张**");
        assertThat(vo.getStatusDesc()).isEqualTo("待缴费");
        assertThat(vo.getDiagnosis()).isEqualTo("上呼吸道感染");
    }

    private Prescription prescriptionOf(Long id, Long patientId) {
        Prescription prescription = new Prescription();
        prescription.setId(id);
        prescription.setPatientId(patientId);
        prescription.setDoctorId(200L);
        prescription.setMedicalRecordId(300L);
        prescription.setStatus(0);
        return prescription;
    }

    private MedicalRecord recordOf(Long id) {
        MedicalRecord record = new MedicalRecord();
        record.setId(id);
        record.setDiagnosis("上呼吸道感染");
        return record;
    }
}
