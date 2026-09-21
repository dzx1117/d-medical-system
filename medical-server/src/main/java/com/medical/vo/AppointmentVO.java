package com.medical.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentVO {

    private Long id;

    private String appointmentNo;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String departmentName;

    private LocalDate appointmentDate;

    private Integer timeSlot;

    private String timeSlotDesc;

    private BigDecimal fee;

    private Integer status;

    private String statusDesc;

    private LocalDateTime visitTime;

    private LocalDateTime createTime;
}
