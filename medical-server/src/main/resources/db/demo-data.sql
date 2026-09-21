-- ====================================================================
-- 演示数据补充脚本（可选，在 schema.sql 之后执行）
--
-- 作用：为未来 7 天生成一批预约记录，让管理看板的
--       「未来 7 天就诊预约分布」「科室就诊量」「时段分布」有数据可展示。
--
-- 幂等：重复执行不会产生重复预约（预约单号唯一）。
--
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 < demo-data.sql
-- ====================================================================

USE medical_system;

-- 1. 把排班铺满未来 7 天（schema.sql 只生成未来 4 天）
INSERT INTO schedule (doctor_id, schedule_date, time_slot, total_slots, booked_slots, fee, status)
SELECT d.id, DATE_ADD(CURDATE(), INTERVAL n.offset DAY), t.slot, 20, 0, d.fee, 1
FROM doctor d
         CROSS JOIN (SELECT 4 AS offset UNION SELECT 5 UNION SELECT 6) n
         CROSS JOIN (SELECT 1 AS slot UNION SELECT 2) t
WHERE NOT EXISTS (
    SELECT 1 FROM schedule s
    WHERE s.doctor_id = d.id
      AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL n.offset DAY)
      AND s.time_slot = t.slot
      AND s.deleted = 0
);

-- 2. 为未来 7 天的部分号源生成预约
--    预约单号 = DEMO + 月日 + 医生ID(2位) + 时段，保证唯一，可重复执行
INSERT INTO appointment
    (appointment_no, patient_id, doctor_id, schedule_id, appointment_date, time_slot, fee, status, create_time)
SELECT CONCAT('DEMO', DATE_FORMAT(s.schedule_date, '%m%d'), LPAD(s.doctor_id, 2, '0'), s.time_slot),
       (SELECT id FROM sys_user WHERE username = IF(s.id % 2 = 0, 'patient001', 'patient002')),
       s.doctor_id,
       s.id,
       s.schedule_date,
       s.time_slot,
       s.fee,
       0,
       DATE_SUB(NOW(), INTERVAL (s.id % 5) DAY)
FROM schedule s
WHERE s.schedule_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 6 DAY)
  AND s.id % 3 <> 0
  AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.appointment_no = CONCAT('DEMO', DATE_FORMAT(s.schedule_date, '%m%d'), LPAD(s.doctor_id, 2, '0'), s.time_slot));

-- 3. 把其中几天标记为已完成就诊，让「已完成」「医生工作量」有数据
UPDATE appointment
SET status = 1, visit_time = TIMESTAMP(appointment_date, '10:30:00')
WHERE appointment_date = CURDATE();

-- 4. 按实际预约记录重新校准号源占用数
UPDATE schedule s
SET s.booked_slots = (SELECT COUNT(*) FROM appointment a WHERE a.schedule_id = s.id AND a.status = 0);

SELECT '演示数据写入完成' AS status;
SELECT (SELECT COUNT(*) FROM appointment) AS appointments,
       (SELECT COUNT(*) FROM schedule)    AS schedules;
