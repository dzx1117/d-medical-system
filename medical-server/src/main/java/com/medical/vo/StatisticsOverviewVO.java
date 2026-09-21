package com.medical.vo;

import lombok.Data;

@Data
public class StatisticsOverviewVO {

    /** 预约总数 */
    private Long totalAppointments;

    /** 今日就诊预约数 */
    private Long todayAppointments;

    /** 待就诊 */
    private Long pendingAppointments;

    /** 已完成就诊 */
    private Long completedAppointments;

    /** 已取消 */
    private Long cancelledAppointments;

    /** 医生总数 */
    private Long totalDoctors;

    /** 患者总数 */
    private Long totalPatients;

    /** 科室总数 */
    private Long totalDepartments;

    /** 病历总数 */
    private Long totalRecords;

    /** 处方总数 */
    private Long totalPrescriptions;

    /** 处方总金额 */
    private java.math.BigDecimal totalPrescriptionAmount;
}
