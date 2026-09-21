package com.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DoctorDTO {

    private Long id;

    private Long userId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    @NotNull(message = "请选择所属科室")
    private Long departmentId;

    @NotBlank(message = "职称不能为空")
    private String title;

    private String specialty;

    private String introduction;

    private BigDecimal fee;
}
