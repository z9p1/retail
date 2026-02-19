# 下单相关接口代码复查

## 一、复查结论汇总

| 项           | 结论     | 说明 |
|--------------|----------|------|
| 库存校验     | ✅ 已具备 | 创建订单与支付环节均有校验，支付用乐观锁防超卖 |
| 防重复提交   | ✅ 已具备 | 创建订单支持幂等键；支付/取消/发货均为幂等 |
| 权限控制     | ✅ 已具备 | 需登录 + 仅可操作本人订单，店家接口单独校验角色 |

---

## 二、库存校验

### 2.1 创建订单（下单）`POST /api/user/orders`

- **位置**：`OrderService.createOrder`
- **逻辑**：对每个商品逐项校验  
  - 商品存在、在售（`ON_SALE`）、数量 &gt; 0  
  - **库存**：`p.getStock() < dto.getQuantity()` 时抛出 `STOCK_INSUFFICIENT`
- **说明**：仅做校验，不扣减库存；扣减在支付时进行。

### 2.2 支付 `POST /api/user/orders/{orderId}/pay`

- **位置**：`OrderService.pay`
- **逻辑**：按订单明细扣减库存，使用 **乐观锁**（`ProductMapper.deductStock`：`WHERE id=? AND version=? AND stock>=?`），影响行数为 0 则抛库存不足并回滚。
- **说明**：防止并发超卖，符合 README 4.5 库存扣减策略。

---

## 三、防重复提交（幂等）

### 3.1 创建订单

- **原状**：无幂等键时，重复请求会多次生成订单。
- **已做**：  
  - 支持请求头 **`Idempotency-Key`**（可选）。  
  - 同一 key 在 **5 分钟内**再次请求时，直接返回已创建订单，不新建。  
  - 使用 Redis：`order:idempotency:{key}` → orderId，TTL 300 秒。
- **前端**：购物车「去结算」时生成 UUID 作为幂等键并传入，且按钮在请求期间禁用，减少重复点击。

### 3.2 支付

- **位置**：`OrderService.pay`
- **逻辑**：若订单状态已是 `PENDING_SHIP` / `SHIPPED` / `COMPLETED`，直接 `return`，不重复扣减、不报错。
- **结论**：重复支付请求幂等。

### 3.3 取消订单

- **逻辑**：若已是 `CANCELLED`，直接 `return`。  
- **结论**：幂等。

### 3.4 发货

- **逻辑**：若已是 `SHIPPED` / `COMPLETED`，直接 `return`。  
- **结论**：幂等。

---

## 四、权限控制

### 4.1 接口是否需登录

- **下单/支付/取消/查单**：路径为 `/api/user/*`，**不在** JWT 白名单内。
- **JwtAuthFilter**：对非白名单请求校验 `Authorization: Bearer <token>`，无效或缺失则 401。
- **Controller**：`WebUtil.getUserId(request)` 为 null 时返回 401「未登录」。
- **结论**：未登录无法下单或操作订单。

### 4.2 是否只能操作本人订单

- **支付 / 取消 / 确认收货**：`order.getUserId().equals(userId)`，userId 来自 JWT，不匹配则 403「无权限」。
- **订单详情**：`order.getUserId().equals(userId)` 不匹配则 403。
- **结论**：用户只能操作自己的订单，不能代他人支付/取消/确认。

### 4.3 店家与用户接口隔离

- 店家订单列表/发货等为 `/api/store/*`，JwtAuthFilter 对 `STORE_PREFIX` 要求角色为 **STORE**，否则 403。
- 用户下单、支付等为 `/api/user/*`，仅校验登录，不要求 STORE。
- **结论**：角色隔离符合需求。

---

## 五、建议（可选）

1. **Idempotency-Key**：当前为可选；若希望所有创建订单请求都防重复，可改为必填，无 key 时返回 400。
2. **限流**：可对 `POST /api/user/orders` 做按用户限流，降低恶意刷单风险。
3. **审计**：重要操作（下单、支付、取消）可打日志（订单号、userId、时间等），便于对账与排查。

---

*复查日期：按代码版本为准*
