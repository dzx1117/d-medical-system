CREATE DATABASE IF NOT EXISTS medical_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE medical_system;

-- 用户基础表
CREATE TABLE sys_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '加密密码',
    real_name   VARCHAR(64)  NULL COMMENT '真实姓名',
    phone       VARCHAR(20)  NULL COMMENT '手机号',
    email       VARCHAR(100) NULL COMMENT '邮箱',
    id_card     VARCHAR(18)  NULL COMMENT '身份证号',
    user_type   TINYINT      NOT NULL DEFAULT 0 COMMENT '用户类型：0患者，1医生，2管理员',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_user_type (user_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户基础表';

-- 角色表
CREATE TABLE sys_role
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL UNIQUE COMMENT '角色编码',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 权限表
CREATE TABLE sys_permission
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(64)  NOT NULL UNIQUE COMMENT '权限编码',
    permission_name VARCHAR(64)  NOT NULL COMMENT '权限名称',
    resource_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '资源类型：1菜单，2按钮',
    parent_id     BIGINT       NULL COMMENT '父权限ID',
    path          VARCHAR(255) NULL COMMENT '路由路径',
    sort_order    INT          NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

-- 用户角色关联表
CREATE TABLE sys_user_role
(
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- 角色权限关联表
CREATE TABLE sys_role_permission
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';

-- 科室表
CREATE TABLE department
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_code   VARCHAR(64)  NOT NULL UNIQUE COMMENT '科室编码',
    dept_name   VARCHAR(128) NOT NULL COMMENT '科室名称',
    description VARCHAR(500) NULL COMMENT '科室介绍',
    location    VARCHAR(255) NULL COMMENT '科室位置',
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='科室表';

-- 医生信息表
CREATE TABLE doctor
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL UNIQUE COMMENT '关联用户ID',
    department_id BIGINT      NOT NULL COMMENT '所属科室',
    title        VARCHAR(64)  NOT NULL COMMENT '职称：主任医师/副主任医师/主治医师等',
    specialty    VARCHAR(500) NULL COMMENT '擅长领域',
    introduction VARCHAR(1000) NULL COMMENT '医生简介',
    fee          DECIMAL(10, 2) DEFAULT 0.00 COMMENT '挂号费',
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dept (department_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='医生信息表';

-- 排班/号源表
CREATE TABLE schedule
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    doctor_id      BIGINT       NOT NULL COMMENT '医生ID',
    schedule_date  DATE         NOT NULL COMMENT '出诊日期',
    time_slot      TINYINT      NOT NULL COMMENT '时段：1上午，2下午，3晚上',
    total_slots    INT          NOT NULL DEFAULT 0 COMMENT '总号源数',
    booked_slots   INT          NOT NULL DEFAULT 0 COMMENT '已预约数',
    fee            DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停诊，1正常',
    deleted        TINYINT      NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doctor_date_slot (doctor_id, schedule_date, time_slot),
    INDEX idx_date (schedule_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='医生排班表';

-- 预约订单表
CREATE TABLE appointment
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    appointment_no  VARCHAR(32)  NOT NULL UNIQUE COMMENT '预约单号',
    patient_id      BIGINT       NOT NULL COMMENT '患者ID',
    doctor_id       BIGINT       NOT NULL COMMENT '医生ID',
    schedule_id     BIGINT       NOT NULL COMMENT '排班ID',
    appointment_date DATE        NOT NULL COMMENT '预约日期',
    time_slot       TINYINT      NOT NULL COMMENT '时段',
    fee             DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待就诊，1已完成，2已取消',
    visit_time      DATETIME     NULL COMMENT '实际就诊时间',
    cancel_time     DATETIME     NULL COMMENT '取消时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_date (appointment_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='预约订单表';

-- 电子病历表
CREATE TABLE medical_record
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id   BIGINT       NOT NULL COMMENT '患者ID',
    doctor_id    BIGINT       NOT NULL COMMENT '医生ID',
    appointment_id BIGINT     NOT NULL COMMENT '关联预约ID',
    visit_date   DATE         NOT NULL COMMENT '就诊日期',
    chief_complaint VARCHAR(1000) NULL COMMENT '主诉',
    present_illness TEXT      NULL COMMENT '现病史',
    diagnosis    VARCHAR(500) NULL COMMENT '诊断结果',
    treatment_plan TEXT       NULL COMMENT '治疗方案',
    remark       VARCHAR(1000) NULL COMMENT '备注',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='电子病历表';

-- 处方主表
CREATE TABLE prescription
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_no VARCHAR(32)  NOT NULL UNIQUE COMMENT '处方单号',
    patient_id      BIGINT       NOT NULL COMMENT '患者ID',
    doctor_id       BIGINT       NOT NULL COMMENT '医生ID',
    medical_record_id BIGINT     NOT NULL COMMENT '关联病历ID',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待缴费，1已缴费，2已取药，3已取消',
    total_amount    DECIMAL(10, 2) DEFAULT 0.00 COMMENT '总金额',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='处方主表';

-- 处方明细表
CREATE TABLE prescription_item
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT       NOT NULL COMMENT '处方ID',
    medicine_id     BIGINT       NOT NULL COMMENT '药品ID',
    medicine_name   VARCHAR(255) NOT NULL COMMENT '药品名称',
    dosage          VARCHAR(255) NULL COMMENT '剂量',
    frequency       VARCHAR(255) NULL COMMENT '用药频次',
    duration        VARCHAR(255) NULL COMMENT '用药时长',
    quantity        INT          NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price      DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    total_price     DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '小计',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='处方明细表';

-- 药品字典表
CREATE TABLE medicine
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL UNIQUE COMMENT '药品编码',
    name        VARCHAR(255) NOT NULL COMMENT '药品名称',
    category    VARCHAR(64)  NULL COMMENT '药品分类',
    spec        VARCHAR(255) NULL COMMENT '规格',
    unit        VARCHAR(64)  NULL COMMENT '单位',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    stock       INT          NOT NULL DEFAULT 0 COMMENT '库存',
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='药品字典表';

-- 初始化角色数据
INSERT INTO sys_role (role_code, role_name, description) VALUES
('patient', '患者', '可预约挂号、查看病历'),
('doctor', '医生', '可管理排班、就诊、开处方'),
('admin', '管理员', '系统管理');

-- 初始化管理员账号：admin / admin123
INSERT INTO sys_user (username, password, real_name, phone, user_type, status) VALUES
('admin', '$2a$10$2OC/vmlIyLhNb9rXZHcVE.oIzb9/EcHxfDSahAXMMjAOcTIowWLq2', '系统管理员', '13800000000', 2, 1);


-- ====================================================================
-- 演示数据（以下所有演示账号密码统一为 123456）
-- ====================================================================

INSERT INTO department (dept_code, dept_name, description, location, status) VALUES
('DEPT001', '内科', '诊治呼吸、消化、心血管等内科系统疾病', '门诊楼2层东侧', 1),
('DEPT002', '外科', '诊治需要手术治疗的各类外科疾病', '门诊楼3层东侧', 1),
('DEPT003', '儿科', '0-14岁儿童常见病、多发病的诊治', '门诊楼1层西侧', 1),
('DEPT004', '骨科', '骨骼、关节、肌肉系统疾病诊治', '门诊楼3层西侧', 1),
('DEPT005', '眼科', '眼部疾病诊断与治疗', '门诊楼4层', 1);

-- 医生账号
INSERT INTO sys_user (username, password, real_name, phone, user_type, status) VALUES
('doctor001', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '张伟', '13900000001', 1, 1),
('doctor002', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '李娜', '13900000002', 1, 1),
('doctor003', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '王强', '13900000003', 1, 1),
('doctor004', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '刘洋', '13900000004', 1, 1),
('doctor005', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '陈静', '13900000005', 1, 1);

-- 患者账号
INSERT INTO sys_user (username, password, real_name, phone, user_type, status) VALUES
('patient001', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '赵小明', '13700000001', 0, 1),
('patient002', '$2a$10$YHJVhN97I5zmVuoJaqQPg.YETsSpEdO/XisUuBnx2326jXr9uuyO6', '孙丽丽', '13700000002', 0, 1);

-- 医生档案（通过用户名关联，避免依赖自增 ID 的具体取值）
INSERT INTO doctor (user_id, department_id, title, specialty, introduction, fee, status)
SELECT u.id, d.id, '主任医师', '高血压、冠心病、心律失常', '从事内科临床工作 25 年，擅长心血管疾病的诊治。', 50.00, 1
FROM sys_user u, department d WHERE u.username = 'doctor001' AND d.dept_code = 'DEPT001';

INSERT INTO doctor (user_id, department_id, title, specialty, introduction, fee, status)
SELECT u.id, d.id, '主治医师', '胃炎、消化性溃疡、功能性消化不良', '专注消化系统疾病诊治，注重患者生活方式指导。', 20.00, 1
FROM sys_user u, department d WHERE u.username = 'doctor002' AND d.dept_code = 'DEPT001';

INSERT INTO doctor (user_id, department_id, title, specialty, introduction, fee, status)
SELECT u.id, d.id, '副主任医师', '胆囊结石、阑尾炎、疝气', '擅长普外科微创手术，年手术量 300 余台。', 35.00, 1
FROM sys_user u, department d WHERE u.username = 'doctor003' AND d.dept_code = 'DEPT002';

INSERT INTO doctor (user_id, department_id, title, specialty, introduction, fee, status)
SELECT u.id, d.id, '主治医师', '小儿呼吸道感染、腹泻、发热', '擅长儿科常见病诊治，善于与患儿沟通。', 20.00, 1
FROM sys_user u, department d WHERE u.username = 'doctor004' AND d.dept_code = 'DEPT003';

INSERT INTO doctor (user_id, department_id, title, specialty, introduction, fee, status)
SELECT u.id, d.id, '主任医师', '白内障、青光眼、眼底病', '从事眼科临床与科研工作 20 年。', 50.00, 1
FROM sys_user u, department d WHERE u.username = 'doctor005' AND d.dept_code = 'DEPT005';

-- 药品字典
INSERT INTO medicine (code, name, category, spec, unit, price, stock, status) VALUES
('MED001', '阿莫西林胶囊', '抗生素', '0.25g*24粒', '盒', 18.50, 300, 1),
('MED002', '布洛芬缓释胶囊', '解热镇痛', '0.3g*20粒', '盒', 22.00, 250, 1),
('MED003', '头孢克肟分散片', '抗生素', '0.1g*12片', '盒', 36.80, 180, 1),
('MED004', '感冒灵颗粒', '中成药', '10g*9袋', '盒', 15.00, 400, 1),
('MED005', '维生素C片', '维生素类', '100mg*100片', '瓶', 8.50, 500, 1),
('MED006', '奥美拉唑肠溶胶囊', '消化系统', '20mg*14粒', '盒', 28.00, 220, 1),
('MED007', '蒙脱石散', '消化系统', '3g*10袋', '盒', 19.90, 160, 1),
('MED008', '氯雷他定片', '抗过敏', '10mg*6片', '盒', 16.50, 200, 1),
('MED009', '硝苯地平缓释片', '心血管', '20mg*30片', '盒', 32.00, 150, 1),
('MED010', '复方甘草片', '镇咳祛痰', '100片', '瓶', 9.80, 260, 1);

-- 近几天的排班（日期基于当前日期动态生成，保证演示时号源未过期）
INSERT INTO schedule (doctor_id, schedule_date, time_slot, total_slots, booked_slots, fee, status)
SELECT d.id, DATE_ADD(CURDATE(), INTERVAL n.offset DAY), t.slot, 20, 0, d.fee, 1
FROM doctor d
         CROSS JOIN (SELECT 0 AS offset UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) n
         CROSS JOIN (SELECT 1 AS slot UNION SELECT 2) t;

-- 角色与用户绑定
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE r.role_code = 'admin' AND u.username = 'admin';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE r.role_code = 'doctor' AND u.user_type = 1;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE r.role_code = 'patient' AND u.user_type = 0;
