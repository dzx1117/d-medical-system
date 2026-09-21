package com.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appointmentNo;

    private Long patientId;

    private Long doctorId;

    private Long scheduleId;

    private LocalDate appointmentDate;

    private Integer timeSlot;

    private BigDecimal fee;

    private Integer status;

    private LocalDateTime visitTime;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
