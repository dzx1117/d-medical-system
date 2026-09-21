package com.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription_item")
public class PrescriptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long prescriptionId;

    private Long medicineId;

    private String medicineName;

    private String dosage;

    private String frequency;

    private String duration;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private LocalDateTime createTime;
}
