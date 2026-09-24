package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.dto.PrescriptionDTO;
import com.medical.entity.*;
import com.medical.mapper.*;
import com.medical.service.PrescriptionService;
import com.medical.vo.PrescriptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicineMapper medicineMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO createPrescription(Long doctorId, PrescriptionDTO dto) {
        MedicalRecord record = medicalRecordMapper.selectById(dto.getMedicalRecordId());
        if (record == null) {
            throw new BusinessException("病历不存在");
        }
        if (!record.getDoctorId().equals(doctorId)) {
            throw new BusinessException("只能为自己的患者开处方");
        }

        LambdaQueryWrapper<Prescription> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(Prescription::getMedicalRecordId, dto.getMedicalRecordId());
        if (prescriptionMapper.selectOne(existsWrapper) != null) {
            throw new BusinessException("该病历已开过处方");
        }

        // 汇总同一种药品的数量，避免同一处方里重复出现同一药品导致库存校验失真
        Map<Long, Integer> quantityByMedicine = new LinkedHashMap<>();
        for (PrescriptionDTO.Item item : dto.getItems()) {
            quantityByMedicine.merge(item.getMedicineId(), item.getQuantity(), Integer::sum);
        }

        List<Long> medicineIds = new ArrayList<>(quantityByMedicine.keySet());
        Map<Long, Medicine> medicineMap = medicineMapper.selectBatchIds(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PrescriptionItem> items = new ArrayList<>();

        for (PrescriptionDTO.Item item : dto.getItems()) {
            Medicine medicine = medicineMap.get(item.getMedicineId());
            if (medicine == null) {
                throw new BusinessException("药品不存在: id=" + item.getMedicineId());
            }

            PrescriptionItem entity = new PrescriptionItem();
            entity.setMedicineId(medicine.getId());
            entity.setMedicineName(medicine.getName());
            entity.setDosage(item.getDosage());
            entity.setFrequency(item.getFrequency());
            entity.setDuration(item.getDuration());
            entity.setQuantity(item.getQuantity());
            entity.setUnitPrice(medicine.getPrice());
            BigDecimal subtotal = medicine.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            entity.setTotalPrice(subtotal);
            items.add(entity);

            totalAmount = totalAmount.add(subtotal);
        }

        // 原子扣减库存：任一药品库存不足则整体回滚
        for (Map.Entry<Long, Integer> entry : quantityByMedicine.entrySet()) {
            int affected = medicineMapper.deductStock(entry.getKey(), entry.getValue());
            if (affected == 0) {
                Medicine medicine = medicineMap.get(entry.getKey());
                throw new BusinessException("药品库存不足：" + (medicine == null ? entry.getKey() : medicine.getName()));
            }
        }

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(generatePrescriptionNo());
        prescription.setPatientId(record.getPatientId());
        prescription.setDoctorId(doctorId);
        prescription.setMedicalRecordId(dto.getMedicalRecordId());
        prescription.setStatus(0);
        prescription.setTotalAmount(totalAmount);
        prescriptionMapper.insert(prescription);

        for (PrescriptionItem item : items) {
            item.setPrescriptionId(prescription.getId());
            prescriptionItemMapper.insert(item);
        }

        log.info("开处方成功: prescriptionNo={}, doctorId={}, totalAmount={}",
                prescription.getPrescriptionNo(), doctorId, totalAmount);

        return convertList(Collections.singletonList(prescription)).get(0);
    }

    @Override
    public Page<PrescriptionVO> pagePatientPrescriptions(Long patientId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getPatientId, patientId).orderByDesc(Prescription::getCreateTime);
        Page<Prescription> page = prescriptionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(page);
    }

    @Override
    public Page<PrescriptionVO> pageDoctorPrescriptions(Long doctorId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getDoctorId, doctorId).orderByDesc(Prescription::getCreateTime);
        Page<Prescription> page = prescriptionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(page);
    }

    @Override
    public PrescriptionVO getVOById(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        return convertList(Collections.singletonList(prescription)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payPrescription(Long patientId, Long prescriptionId) {
        // 归属校验仍然先做：不是本人的处方直接拒绝，不必走到状态更新
        getOwnedPrescription(patientId, prescriptionId);
        // 状态校验交给数据库：判断与更新在同一条 SQL 里，并发下只有一个请求能拿到影响行数
        if (prescriptionMapper.payIfPending(prescriptionId) == 0) {
            throw new BusinessException("当前状态无法缴费");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickupPrescription(Long patientId, Long prescriptionId) {
        getOwnedPrescription(patientId, prescriptionId);
        if (prescriptionMapper.pickupIfPaid(prescriptionId) == 0) {
            throw new BusinessException("请先完成缴费");
        }
    }

    private Prescription getOwnedPrescription(Long patientId, Long prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (!prescription.getPatientId().equals(patientId)) {
            throw new BusinessException("无权操作他人的处方");
        }
        return prescription;
    }

    private String generatePrescriptionNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "RX" + timestamp + random;
    }

    private Page<PrescriptionVO> convertPage(Page<Prescription> page) {
        Page<PrescriptionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(convertList(page.getRecords()));
        return voPage;
    }

    private List<PrescriptionVO> convertList(List<Prescription> prescriptions) {
        if (prescriptions == null || prescriptions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> prescriptionIds = prescriptions.stream().map(Prescription::getId).collect(Collectors.toList());
        List<Long> patientIds = prescriptions.stream().map(Prescription::getPatientId).distinct().collect(Collectors.toList());
        List<Long> doctorIds = prescriptions.stream().map(Prescription::getDoctorId).distinct().collect(Collectors.toList());
        List<Long> recordIds = prescriptions.stream().map(Prescription::getMedicalRecordId).distinct().collect(Collectors.toList());

        Map<Long, List<PrescriptionItem>> itemMap = prescriptionItemMapper.selectList(
                        new LambdaQueryWrapper<PrescriptionItem>().in(PrescriptionItem::getPrescriptionId, prescriptionIds))
                .stream().collect(Collectors.groupingBy(PrescriptionItem::getPrescriptionId));

        // 明细里没有冗余规格/单位，这里按药品 ID 回查
        Set<Long> medicineIds = itemMap.values().stream()
                .flatMap(List::stream)
                .map(PrescriptionItem::getMedicineId)
                .collect(Collectors.toSet());
        Map<Long, Medicine> medicineMap = medicineIds.isEmpty() ? Collections.emptyMap()
                : medicineMapper.selectBatchIds(medicineIds).stream()
                    .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        Map<Long, User> patientMap = userMapper.selectBatchIds(patientIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Doctor> doctors = doctorMapper.selectBatchIds(doctorIds);
        Map<Long, Doctor> doctorMap = doctors.stream().collect(Collectors.toMap(Doctor::getId, Function.identity()));

        List<Long> doctorUserIds = doctors.stream().map(Doctor::getUserId).collect(Collectors.toList());
        Map<Long, User> doctorUserMap = doctorUserIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(doctorUserIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, MedicalRecord> recordMap = medicalRecordMapper.selectBatchIds(recordIds).stream()
                .collect(Collectors.toMap(MedicalRecord::getId, Function.identity()));

        return prescriptions.stream().map(prescription -> {
            PrescriptionVO vo = new PrescriptionVO();
            vo.setId(prescription.getId());
            vo.setPrescriptionNo(prescription.getPrescriptionNo());
            vo.setPatientId(prescription.getPatientId());
            vo.setDoctorId(prescription.getDoctorId());
            vo.setMedicalRecordId(prescription.getMedicalRecordId());
            vo.setStatus(prescription.getStatus());
            vo.setStatusDesc(statusDesc(prescription.getStatus()));
            vo.setTotalAmount(prescription.getTotalAmount());
            vo.setCreateTime(prescription.getCreateTime());

            User patient = patientMap.get(prescription.getPatientId());
            if (patient != null) {
                vo.setPatientName(maskName(patient.getRealName()));
            }

            Doctor doctor = doctorMap.get(prescription.getDoctorId());
            if (doctor != null) {
                User doctorUser = doctorUserMap.get(doctor.getUserId());
                if (doctorUser != null) {
                    vo.setDoctorName(doctorUser.getRealName());
                }
            }

            MedicalRecord record = recordMap.get(prescription.getMedicalRecordId());
            if (record != null) {
                vo.setDiagnosis(record.getDiagnosis());
            }

            List<PrescriptionItem> items = itemMap.getOrDefault(prescription.getId(), Collections.emptyList());
            vo.setItems(items.stream().map(item -> {
                PrescriptionVO.ItemVO itemVO = new PrescriptionVO.ItemVO();
                itemVO.setId(item.getId());
                itemVO.setMedicineId(item.getMedicineId());
                itemVO.setMedicineName(item.getMedicineName());
                itemVO.setDosage(item.getDosage());
                itemVO.setFrequency(item.getFrequency());
                itemVO.setDuration(item.getDuration());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setUnitPrice(item.getUnitPrice());
                itemVO.setTotalPrice(item.getTotalPrice());

                Medicine medicine = medicineMap.get(item.getMedicineId());
                if (medicine != null) {
                    itemVO.setSpec(medicine.getSpec());
                    itemVO.setUnit(medicine.getUnit());
                }
                return itemVO;
            }).collect(Collectors.toList()));

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

    private String statusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待缴费";
            case 1 -> "已缴费";
            case 2 -> "已取药";
            case 3 -> "已取消";
            default -> "未知";
        };
    }
}
