package com.medical.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MedicalRecordVO {

    private Long id;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String departmentName;

    private Long appointmentId;

    private LocalDate visitDate;

    private String chiefComplaint;

    private String presentIllness;

    private String diagnosis;

    private String treatmentPlan;

    private String remark;

    private LocalDateTime createTime;
}
