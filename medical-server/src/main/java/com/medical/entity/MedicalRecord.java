package com.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long doctorId;

    private Long appointmentId;

    private LocalDate visitDate;

    private String chiefComplaint;

    private String presentIllness;

    private String diagnosis;

    private String treatmentPlan;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
