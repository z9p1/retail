# 构建与运行说明

## 环境要求

- JDK 1.8
- Maven 3.x
- MySQL 8.x（本地）
- Redis（本地）
- Node.js 18+（前端）

## 数据库初始化

```bash
# 创建库并执行建表
mysql -u root -p < retail-backend/src/main/resources/schema.sql
```

修改 `retail-backend/src/main/resources/application.yml` 中的数据库账号密码（默认 root/root）。

## 后端

**注意**：必须在 `retail-backend` 目录下执行 Maven，根目录没有 pom.xml 会报错 `No plugin found for prefix 'spring-boot'`。

```bash
cd retail-backend
mvn spring-boot:run
```

后端启动在 http://localhost:8080

## 前端

```bash
cd retail-frontend
npm install
npm run dev
```

前端开发服务器在 http://localhost:5173，接口通过 Vite 代理到 8080。

## 首次使用

1. 执行 `ddl.sql` 建表后执行 `dml.sql` 初始化数据（含 5 商品、5 客户、10 条订单及**店家账号**）。
2. **店家默认账号**：用户名 `store`，密码 `admin123`（仅在使用 dml.sql 初始化后可用）。
3. 客户账号：`customer1`～`customer5`，密码均为 `123456`。
4. 店家登录后：工作台（含补货提醒）、商品管理、订单管理、流量监控、用户分析、**定时任务**（可开启 customer1 每小时模拟购买）、设置。
5. 用户登录后：商城、购物车、我的订单、我的。

## 接口前缀与权限

- 白名单（无需登录）：`/api/auth/login`、`/api/auth/register`、`/api/user/products/**`
- 需登录（用户或店家）：`/api/user/orders/**`、`/api/store/workbench` 等
- 仅店家：`/api/store/**`、`/api/traffic`、`/api/user-analysis/**`
Get-Content retail-backend\src\main\resources\schema.sql | mysql -u root -p