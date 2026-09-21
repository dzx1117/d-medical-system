package com.medical.mapper;

import com.medical.vo.NameValueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT COUNT(*) FROM appointment")
    Long countAllAppointments();

    @Select("SELECT COUNT(*) FROM appointment WHERE appointment_date = CURDATE() AND status <> 2")
    Long countTodayAppointments();

    @Select("SELECT COUNT(*) FROM appointment WHERE status = #{status}")
    Long countAppointmentsByStatus(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM doctor WHERE deleted = 0")
    Long countDoctors();

    @Select("SELECT COUNT(*) FROM sys_user WHERE user_type = 0 AND deleted = 0")
    Long countPatients();

    @Select("SELECT COUNT(*) FROM department WHERE deleted = 0")
    Long countDepartments();

    @Select("SELECT COUNT(*) FROM medical_record")
    Long countRecords();

    @Select("SELECT COUNT(*) FROM prescription")
    Long countPrescriptions();

    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM prescription WHERE status <> 3")
    BigDecimal sumPrescriptionAmount();

    /** 各科室已完成就诊量排行 */
    @Select("""
            SELECT d.dept_name AS name, COUNT(a.id) AS value
            FROM appointment a
            JOIN doctor doc ON a.doctor_id = doc.id
            JOIN department d ON doc.department_id = d.id
            WHERE a.status = 1
            GROUP BY d.id, d.dept_name
            ORDER BY value DESC
            LIMIT 10
            """)
    List<NameValueVO> countByDepartment();

    /** 医生工作量排行（已完成就诊数） */
    @Select("""
            SELECT u.real_name AS name, COUNT(a.id) AS value
            FROM appointment a
            JOIN doctor doc ON a.doctor_id = doc.id
            JOIN sys_user u ON doc.user_id = u.id
            WHERE a.status = 1
            GROUP BY doc.id, u.real_name
            ORDER BY value DESC
            LIMIT 10
            """)
    List<NameValueVO> countByDoctor();

    /** 未来 7 天按就诊日期的预约量（用于号源负载预判） */
    @Select("""
            SELECT DATE_FORMAT(appointment_date, '%Y-%m-%d') AS name, COUNT(*) AS value
            FROM appointment
            WHERE appointment_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 6 DAY)
              AND status <> 2
            GROUP BY appointment_date
            ORDER BY appointment_date
            """)
    List<NameValueVO> countUpcomingTrend();

    /** 预约时段分布 */
    @Select("""
            SELECT
                CASE time_slot
                    WHEN 1 THEN '上午'
                    WHEN 2 THEN '下午'
                    WHEN 3 THEN '晚上'
                    ELSE '未知'
                END AS name,
                COUNT(*) AS value
            FROM appointment
            WHERE status <> 2
            GROUP BY time_slot
            ORDER BY time_slot
            """)
    List<NameValueVO> countByTimeSlot();
}
