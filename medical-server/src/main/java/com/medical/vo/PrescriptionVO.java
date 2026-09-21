package com.medical.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionVO {

    private Long id;

    private String prescriptionNo;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    private Long medicalRecordId;

    private String diagnosis;

    private Integer status;

    private String statusDesc;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;

    private List<ItemVO> items;

    @Data
    public static class ItemVO {

        private Long id;

        private Long medicineId;

        private String medicineName;

        private String spec;

        private String unit;

        private String dosage;

        private String frequency;

        private String duration;

        private Integer quantity;

        private BigDecimal unitPrice;

        private BigDecimal totalPrice;
    }
}
