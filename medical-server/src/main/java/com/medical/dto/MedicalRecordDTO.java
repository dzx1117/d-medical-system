package com.medical.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicalRecordDTO {

    private Long id;

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    @NotNull(message = "就诊日期不能为空")
    private LocalDate visitDate;

    private String chiefComplaint;

    private String presentIllness;

    private String diagnosis;

    private String treatmentPlan;

    private String remark;
}
