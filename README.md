# 智慧医院预约挂号与电子病历管理系统

> 安徽医科大学 · 信息管理与信息系统专业 · 实习项目作品

基于 **Spring Boot 3 + Vue 3 + MySQL + Redis** 的医疗信息化管理系统，覆盖
**预约挂号 → 就诊确认 → 电子病历 → 处方开药 → 缴费取药** 完整业务闭环。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 运行环境 | Java | 17 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 5.0（Windows 便携版） |
| 安全 | Spring Security + JWT | auth0 java-jwt 4.4.0 |
| 接口文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 前端 | Vue 3 + Element Plus + Pinia + Axios | Vue 3.4 / Vite 5 |
| 构建 | Maven 3.9 / npm | - |

## 项目亮点

1. **Redis 分布式锁解决高并发挂号超卖**
   加锁用 `SET key value NX PX ttl` 保证原子性，解锁用 Lua 脚本比对持有者标识避免误删；
   配合数据库 `UPDATE ... WHERE booked_slots < total_slots` 原子更新形成双重防线。
   实测 20 个并发请求抢 5 个号源，最终 `booked_slots = 5`，无超卖。

2. **RBAC 权限模型**
   患者 / 医生 / 管理员三种角色。JWT 中携带角色，映射为 `ROLE_PATIENT` /
   `ROLE_DOCTOR` / `ROLE_ADMIN`，配合 `@PreAuthorize` 与 `SecurityConfig` 做接口级鉴权；
   业务层再做数据归属校验（医生只能看自己的患者、患者只能操作自己的预约）。

3. **医疗数据脱敏**
   患者姓名（张三 → 张\*\*）、手机号（138\*\*\*\*8888）、身份证号在接口返回前统一脱敏。

4. **完整业务闭环**
   挂号 → 就诊 → 病历 → 处方 → 缴费 → 取药，全链路状态机流转，非法状态变更被拦截。

5. **管理数据看板**
   预约总览、未来 7 天就诊负载、科室就诊量排行、医生工作量排行、时段分布。

## 目录结构

```
d-medical-system/
├── medical-server/                 # Spring Boot 后端
│   ├── src/main/java/com/medical/
│   │   ├── common/                 # 统一返回、全局异常、Redis 分布式锁、登录用户工具
│   │   ├── config/                 # MyBatis-Plus / Redis / 跨域 / Knife4j 配置
│   │   ├── security/               # JWT 工具、认证过滤器、Security 配置
│   │   ├── entity/  mapper/        # 11 张表的实体与数据访问层（含 StatisticsMapper）
│   │   ├── dto/  vo/               # 入参与出参模型
│   │   ├── service/                # 业务层（含 AppointmentTxService 事务边界）
│   │   └── controller/             # REST 接口
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/
│   │       ├── schema.sql          # 建表 + 基础数据
│   │       └── demo-data.sql       # 看板演示数据（可选）
│   └── start.bat / start-redis.bat
├── medical-web/                    # Vue 3 前端
│   ├── src/api/                    # 接口封装
│   ├── src/views/                  # 15 个页面
│   ├── src/components/Layout.vue   # 按角色渲染菜单的主框架
│   └── start.bat
└── tests/                          # 可重复执行的测试脚本
    ├── e2e-test-flow.sh            # 业务闭环测试
    ├── e2e-test-concurrency.sh     # 并发防超卖测试
    └── payload/                    # 含中文的 UTF-8 请求体
```

## 快速启动

### 前置依赖

- MySQL 8.0（本机已安装，服务名 `MYSQL80`）
- JDK 17、Maven、Redis —— 项目已在 `../.tools/` 下备好便携版，`start.bat` 会自动引用

### 1. 初始化数据库

```bash
cd medical-server/src/main/resources/db
mysql -uroot -p --default-character-set=utf8mb4 < schema.sql       # 建表 + 基础数据
mysql -uroot -p --default-character-set=utf8mb4 < demo-data.sql    # 可选：排班与预约，填充看板
mysql -uroot -p --default-character-set=utf8mb4 < demo-records.sql # 可选：病历与处方，填充患者/医生端
```

后两个脚本是可选的演示数据，且都是幂等的，重复执行不会产生重复数据。

### 2. 启动 Redis

双击 `medical-server/start-redis.bat`（默认端口 6379）

### 3. 启动后端

双击 `medical-server/start.bat`，看到 `Started MedicalApplication` 即成功。

- 接口地址：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html

### 4. 启动前端

双击 `medical-web/start.bat`（首次会自动 `npm install`）

- 访问地址：http://localhost:5173

---

## Docker 一键启动（推荐）

不需要本地装 JDK / Maven / MySQL / Redis，只要有 Docker 即可。

```bash
cd d-medical-system
cp .env.example .env          # 可选：修改数据库密码和 JWT 密钥
docker compose up -d --build
```

首次启动会自动完成：建库建表 → 灌入演示数据 → 构建并启动前后端。

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost |
| 接口文档 | http://localhost:8080/doc.html |
| MySQL | 容器内 `mysql:3306` |
| Redis | 容器内 `redis:6379` |

常用命令：

```bash
docker compose ps              # 查看状态
docker compose logs -f medical-server   # 跟踪后端日志
docker compose down            # 停止（保留数据）
docker compose down -v         # 停止并清空数据库
```

> 说明：`schema.sql` / `demo-data.sql` / `demo-records.sql` 只在 **mysql 容器首次创建**时执行。
> 想重新灌数据需先 `docker compose down -v`。

## 演示账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `admin` | `admin123` | 科室 / 医生 / 排班 / 药品管理 |
| 医生 | `doctor001` ~ `doctor005` | `123456` | 查看患者、填写病历、开具处方 |
| 患者 | `patient001` / `patient002` | `123456` | 预约挂号、查看病历、缴费取药 |

数据库内置 5 个科室、5 名医生、10 种药品，以及基于当前日期自动生成的未来 4 天排班。

## 核心接口

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | `POST /auth/login` | 登录，返回 JWT |
| 认证 | `POST /auth/register` | 患者注册 |
| 号源 | `GET /schedule/available` | 查询可预约号源 |
| 预约 | `POST /appointment/create?scheduleId=` | **预约挂号（分布式锁）** |
| 预约 | `POST /appointment/cancel/{id}` | 取消预约并释放号源 |
| 预约 | `POST /appointment/complete/{id}` | 医生确认就诊 |
| 病历 | `POST /record` | 医生填写病历 |
| 病历 | `GET /record/my` | 患者查看本人病历 |
| 处方 | `POST /prescription` | 医生开方（原子扣减库存） |
| 处方 | `POST /prescription/pay/{id}` | 患者缴费 |
| 处方 | `POST /prescription/pickup/{id}` | 患者取药 |
| 看板 | `GET /statistics/overview` | 预约总览指标 |
| 看板 | `GET /statistics/upcoming-trend` | 未来 7 天就诊分布 |
| 看板 | `GET /statistics/department-ranking` | 科室就诊量排行 |
| 看板 | `GET /statistics/doctor-ranking` | 医生工作量排行 |

## 权限矩阵

| 接口 | 患者 | 医生 | 管理员 |
|------|:----:|:----:|:------:|
| 预约挂号 / 取消自己的预约 | ✅ | - | - |
| 查看本人病历、处方，缴费取药 | ✅ | - | - |
| 查看预约自己的患者、确认就诊 | - | ✅ | - |
| 填写病历、开具处方 | - | ✅ | - |
| 科室 / 医生 / 排班 / 药品管理 | - | - | ✅ |
| 数据看板 | - | - | ✅ |

## 测试

项目自带两个可重复执行的测试脚本（需后端已启动）。

### 1. 端到端业务闭环测试

```bash
bash tests/e2e-test-flow.sh
```

依次验证：患者登录 → 预约挂号 → 医生查看患者 → 确认就诊 → 填写病历
→ 开具处方（含库存扣减）→ 患者查看病历 → 缴费 → 取药 → 状态机与权限校验。

### 2. 并发防超卖测试

```bash
bash tests/e2e-test-concurrency.sh
```

默认 30 个患者并发抢 5 个号源。实测结果：预约成功 5 次，其余提示"号源已约满"，
数据库 `booked_slots = total_slots = 5`，**无超卖**。

可通过环境变量调整参数：

```bash
N=50 SLOTS=10 bash tests/e2e-test-concurrency.sh
```

## 开发提示

**Windows Git Bash 下用 curl 测试接口时**，命令行参数中的中文会被转换成 GBK
（报错 `Invalid UTF-8 start byte 0xb7`）。解决办法是把请求体写入 UTF-8 文件后
用 `--data-binary @file.json` 传入，而不是内联 `-d '{...中文...}'`。

## 数据库表

| 表名 | 说明 |
|------|------|
| `sys_user` / `sys_role` / `sys_permission` / `sys_user_role` / `sys_role_permission` | 用户与 RBAC 权限 |
| `department` | 科室 |
| `doctor` | 医生档案（关联用户与科室） |
| `schedule` | 医生排班与号源 |
| `appointment` | 预约订单 |
| `medical_record` | 电子病历 |
| `prescription` / `prescription_item` | 处方主表与明细 |
| `medicine` | 药品字典与库存 |
