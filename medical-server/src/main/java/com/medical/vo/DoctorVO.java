package com.medical.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DoctorVO {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private Long departmentId;

    private String departmentName;

    private String title;

    private String specialty;

    private String introduction;

    private BigDecimal fee;

    private Integer status;

    private LocalDateTime createTime;
}
