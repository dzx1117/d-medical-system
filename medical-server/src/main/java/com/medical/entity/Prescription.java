package com.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String prescriptionNo;

    private Long patientId;

    private Long doctorId;

    private Long medicalRecordId;

    private Integer status;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
