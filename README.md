# 店家线上零售系统

面向**单店家**的轻量级线上零售系统：店家管理商品与订单、查看流量与用户分析；用户浏览商城、购物车、下单。支持**店家端智能助手**（自然语言查库存/订单/流量）。技术栈：**Vue3 + Spring Boot + MySQL + Redis**。

---

## 功能概览

### 店家端

| 模块 | 说明 |
|------|------|
| **工作台** | 今日/本周订单数、销售额、待发货数、低库存数、零库存补货提醒；流量卡片与近 7 天销量趋势图 |
| **智能助手** | 自然语言问答：查库存、零库存/低库存、待发货、今日/本周数据；问流量与销售（今日/7 天/30 天 UV、PV、下单笔数、成交金额）。30 秒限流、单条最多 20 字，API Key 存数据库 |
| **商品管理** | 商品列表（全部/上下架）、上架/下架、编辑库存、新增商品 |
| **订单管理** | 订单列表（状态筛选）、发货、订单详情 |
| **流量监控** | 今日/最近 7 天/30 天：UV、PV、下单笔数、下单人数、成交金额；按天汇总与趋势 |
| **用户分析** | 按用户查询：消费汇总、订单明细、商品偏好（结果缓存 5 分钟） |
| **在线用户** | 查看当前在线会话、踢人下线（Redis 会话） |
| **排班 / 设置** | 排班与个人设置、修改密码、退出 |

### 用户端

| 模块 | 说明 |
|------|------|
| **商城** | 在售商品列表、搜索、商品详情、加入购物车、立即购买 |
| **购物车** | 购物车列表、修改数量、删除、去结算 |
| **订单** | 我的订单（待支付/待发货/已发货/已完成等）、去支付、取消、确认收货 |
| **我的** | 个人信息、收货地址、修改密码、退出 |

### 通用

- **登录 / 注册**：JWT 鉴权，店家与用户角色分离；支持异地登录挤掉旧会话（Redis 会话）。
- **下单与支付**：下单仅校验库存与上下架；支付时乐观锁扣减库存，幂等（Idempotency-Key 防重复提交）。
- **定时任务**：整点模拟顾客下单（可关）；待支付超时自动取消并回滚库存。

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3、Vue Router、Pinia、Axios、Vite |
| 后端 | Java 8、Spring Boot 2.7、MyBatis-Plus |
| 数据库 | MySQL 8（商品、订单、用户、访问日志、agent_config 等） |
| 缓存/会话 | Redis（UV 统计、流量/用户分析缓存、会话、限流、幂等） |

---

## 项目结构

```
retail/
├── retail-backend/          # 后端
│   ├── src/main/java/com/retail/
│   │   ├── controller/     # 接口（店家/用户/Agent）
│   │   ├── service/        # 业务（含 StoreAgentService、WorkbenchService）
│   │   ├── entity/ mapper/ # 实体与 MyBatis
│   │   └── ...
│   └── src/main/resources/
│       ├── application.yml
│       ├── schema.sql      # 建表（含 agent_config）
│       ├── ddl.sql
│       └── agent_config_ddl.sql   # 智能助手配置表 DDL（可单独执行）
├── retail-frontend/         # 前端
│   ├── src/
│   │   ├── views/          # 页面（store/ 店家，user/ 用户）
│   │   ├── api/            # 接口封装（含 agent.js）
│   │   ├── stores/         # Pinia（user、agent、cart）
│   │   └── router/
│   └── vite.config.js
└── README.md
```

**说明**：含 API Key 的 DML（如 `dml.sql`、`agent_config_dml.sql`）已加入 `.gitignore`，不提交；部署时需自行执行 DDL/DML 并配置 `agent_config` 表中的 `agent_api_key`。

---

## 本地运行

### 环境要求

- JDK 8、Maven、Node.js 18+
- MySQL 8（库名 `retail`）
- Redis

### 1. 数据库

创建库并执行建表（任选其一）：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS retail CHARACTER SET utf8mb4;"
mysql -u root -p retail < retail-backend/src/main/resources/schema.sql
# 或
mysql -u root -p retail < retail-backend/src/main/resources/ddl.sql
```

智能助手需 `agent_config` 表，若未在 schema/ddl 中，可单独执行：

```bash
mysql -u root -p retail < retail-backend/src/main/resources/agent_config_ddl.sql
```

再执行业务 DML（商品、用户、订单等）；**API Key 请自行写入** `agent_config` 或通过单独 DML 插入（勿提交含真实 Key 的 DML 到仓库）。

### 2. 配置

- **后端** `retail-backend/src/main/resources/application.yml`：  
  修改 `spring.datasource`（url、username、password）、`spring.redis`（host、port）。  
  智能助手：`agent.api-url`、`agent.model`；API Key 从表 `agent_config` 的 `agent_api_key` 读取。
- **前端** `retail-frontend/vite.config.js`：开发时代理 `/api` 到后端（如 `http://localhost:8080`），按需修改。

### 3. 启动

```bash
# 后端
cd retail-backend
mvn spring-boot:run

# 前端（新开终端）
cd retail-frontend
npm install
npm run dev
```

- 后端：<http://localhost:8080>  
- 前端：<http://localhost:5173>

### 4. 默认账号（以实际 DML 为准）

- 店家：`store` / `admin123`
- 用户：`customer1`～`customer5` / `123456`

---

## 智能助手说明

- **入口**：店家登录后进入「工作台」，右下角智能助手面板（默认展开）。
- **能力**：通过 OpenAI 兼容 API + 内置工具查询实时数据：  
  - `get_workbench_summary`：今日/本周订单与销售额、待发货、低库存数、零库存商品列表。  
  - `get_traffic_summary`：指定时间范围（今日/7 天/30 天）的 UV、PV、下单笔数、成交金额等。
- **限制**：单条问题最多 20 字；同一用户 30 秒内仅可调用一次（前后端均校验）。
- **配置**：`agent.api-url`、`agent.model` 在 `application.yml`；API Key 存表 `agent_config`（`config_key='agent_api_key'`），避免写进代码或配置文件。

---

## 文档

- `BUILD.md`：构建与打包说明。  
- `DEPLOY.md`：部署说明（若有）。  
- 业务需求与状态流转等详见仓库内需求/设计文档（如有）。

---

*README 基于当前实现整理，如有差异以代码为准。*
