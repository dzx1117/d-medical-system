-- ====================================================================
-- 为已完成就诊但尚未填写病历的预约补齐「病历 + 处方」，
-- 让两个演示患者账号都有完整的就诊数据可展示。
-- 幂等：已存在病历的预约会被跳过。
-- ====================================================================

USE medical_system;

-- 1. 补病历
INSERT INTO medical_record
    (patient_id, doctor_id, appointment_id, visit_date,
     chief_complaint, present_illness, diagnosis, treatment_plan, remark)
SELECT a.patient_id,
       a.doctor_id,
       a.id,
       a.appointment_date,
       CASE (a.id % 4)
           WHEN 0 THEN '反复上腹疼痛3天，进食后加重'
           WHEN 1 THEN '咳嗽咳痰一周，伴咽痛'
           WHEN 2 THEN '头晕乏力半月，活动后加重'
           ELSE '发热2天，最高体温38.5℃'
       END,
       CASE (a.id % 4)
           WHEN 0 THEN '3天前无明显诱因出现上腹隐痛，伴反酸嗳气，无发热呕吐，自行服药未见缓解。'
           WHEN 1 THEN '一周前受凉后出现咳嗽，咳白色黏痰，伴咽部疼痛，无胸闷气促。'
           WHEN 2 THEN '半月来间断头晕，活动后明显，休息可缓解，无黑矇晕厥。'
           ELSE '2天前出现发热，伴畏寒乏力，无寒战抽搐，无皮疹。'
       END,
       CASE (a.id % 4)
           WHEN 0 THEN '慢性胃炎急性发作'
           WHEN 1 THEN '急性上呼吸道感染'
           WHEN 2 THEN '轻度贫血'
           ELSE '病毒性感染'
       END,
       CASE (a.id % 4)
           WHEN 0 THEN '规律饮食，忌辛辣刺激，按处方服药，注意休息。'
           WHEN 1 THEN '多饮水，注意保暖，按处方服药，症状加重及时复诊。'
           WHEN 2 THEN '加强营养，补充铁剂，两周后复查血常规。'
           ELSE '多休息多饮水，按处方服用退热药物，监测体温。'
       END,
       '建议不适随诊'
FROM appointment a
WHERE a.status = 1
  AND NOT EXISTS (SELECT 1 FROM medical_record m WHERE m.appointment_id = a.id);

-- 2. 为新增的病历补处方（每份病历 2 种药品）
INSERT INTO prescription (prescription_no, patient_id, doctor_id, medical_record_id, status, total_amount)
SELECT CONCAT('RXDEMO', m.id),
       m.patient_id,
       m.doctor_id,
       m.id,
       CASE WHEN m.id % 2 = 0 THEN 2 ELSE 1 END,
       0
FROM medical_record m
WHERE NOT EXISTS (SELECT 1 FROM prescription p WHERE p.medical_record_id = m.id);

-- 3. 处方明细：按病历 ID 轮换搭配两种药
INSERT INTO prescription_item
    (prescription_id, medicine_id, medicine_name, dosage, frequency, duration, quantity, unit_price, total_price)
SELECT p.id, m1.id, m1.name, '1粒', '每日3次', '7天', 2, m1.price, m1.price * 2
FROM prescription p
         JOIN medical_record mr ON p.medical_record_id = mr.id
         JOIN medicine m1 ON m1.id = CASE (mr.id % 4)
                                        WHEN 0 THEN 6
                                        WHEN 1 THEN 4
                                        WHEN 2 THEN 5
                                        ELSE 2
             END
WHERE p.prescription_no LIKE 'RXDEMO%'
  AND (SELECT COUNT(*) FROM prescription_item i WHERE i.prescription_id = p.id) = 0;

INSERT INTO prescription_item
    (prescription_id, medicine_id, medicine_name, dosage, frequency, duration, quantity, unit_price, total_price)
SELECT p.id, m2.id, m2.name, '1粒', '每日1次', '7天', 1, m2.price, m2.price
FROM prescription p
         JOIN medical_record mr ON p.medical_record_id = mr.id
         JOIN medicine m2 ON m2.id = CASE (mr.id % 4)
                                        WHEN 0 THEN 8
                                        WHEN 1 THEN 10
                                        WHEN 2 THEN 8
                                        ELSE 1
             END
WHERE p.prescription_no LIKE 'RXDEMO%'
  AND (SELECT COUNT(*) FROM prescription_item i WHERE i.prescription_id = p.id) = 1;

-- 4. 回填处方总金额
UPDATE prescription p
SET p.total_amount = (SELECT IFNULL(SUM(i.total_price), 0) FROM prescription_item i WHERE i.prescription_id = p.id)
WHERE p.prescription_no LIKE 'RXDEMO%';

-- 5. 扣减对应药品库存（保持数据自洽）
UPDATE medicine med
SET med.stock = GREATEST(0, med.stock - (
    SELECT IFNULL(SUM(i.quantity), 0)
    FROM prescription_item i
             JOIN prescription p ON i.prescription_id = p.id
    WHERE i.medicine_id = med.id AND p.prescription_no LIKE 'RXDEMO%'
))
WHERE med.id IN (
    SELECT DISTINCT i.medicine_id
    FROM prescription_item i
             JOIN prescription p ON i.prescription_id = p.id
    WHERE p.prescription_no LIKE 'RXDEMO%'
);

SELECT '病历与处方补齐完成' AS status;
SELECT (SELECT COUNT(*) FROM medical_record) AS records,
       (SELECT COUNT(*) FROM prescription)   AS prescriptions,
       (SELECT COUNT(*) FROM prescription_item) AS items;
