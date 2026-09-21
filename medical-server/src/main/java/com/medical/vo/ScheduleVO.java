package com.medical.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleVO {

    private Long id;

    private Long doctorId;

    private String doctorName;

    private String title;

    private Long departmentId;

    private String departmentName;

    private LocalDate scheduleDate;

    private Integer timeSlot;

    private String timeSlotDesc;

    private Integer totalSlots;

    private Integer bookedSlots;

    private Integer remainingSlots;

    private BigDecimal fee;

    private Integer status;

    private LocalDateTime createTime;
}
