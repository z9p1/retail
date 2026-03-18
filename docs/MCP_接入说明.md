# MCP 接入说明（把零售系统能力暴露为 MCP 工具）

本项目已提供一个独立的 Node 进程 `mcp-server/`，用于将「零售后端」的能力以 **MCP Tools** 的形式暴露给 Cursor 等支持 MCP 的客户端使用。

> 重要定位：MCP 是 **AI 客户端 ↔ 工具服务** 的协议，适合开发/运营侧让 AI 调用你的业务能力；它不是给浏览器用户直接用的协议，也不会替代你现有的 `Vue + Spring Boot` 业务链路。

---

## 1. 当前已实现的 MCP 工具

`mcp-server/src/index.ts` 已暴露：

- **`get_workbench_summary`**：请求后端 `GET /api/store/workbench`，返回工作台汇总。

依赖：

- 后端可访问（默认 `http://localhost:8080`）
- 店家 JWT（用于调用 `/api/store/workbench`）

---

## 2. 你说的“改成 MCP”通常有两种含义

### 2.1 让 Cursor/IDE 的 AI 能调用你的系统（推荐）

这是 MCP 的典型用法：你在 Cursor 里配置 `mcpServers`，AI 就能在对话中直接调用 `get_workbench_summary` 等工具查数据、生成报告。

**特点**：

- 不影响线上用户链路
- 不需要改前端
- 不需要让 Dify/LLM 直接碰数据库

### 2.2 让“线上智能助手”走 MCP（不推荐直接做）

你的线上助手当前是 **前端 → Spring Boot →（Dify 或 OpenAI 兼容）**。  
如果强行让 Spring Boot 去“调用 MCP 工具”，你需要在 Java 中实现 MCP Client（本仓库当前没有 Java MCP 客户端依赖/实现），复杂度和收益不匹配。

线上助手更常见的企业级做法是：

- **Dify 工作流 HTTP 节点**直接调用后端 REST（配 `X-Internal-Api-Key`）
- 或者仍由 **Java 负责工具调用**（如你现在的 `get_workbench_summary`、`get_product_stock` 工具）

---

## 3. 在 Cursor 中配置 MCP（stdio）

参考 `mcp-server/README.md`，在 Cursor 设置中加入：

```json
{
  "mcpServers": {
    "retail-workbench": {
      "command": "npx",
      "args": ["tsx", "d:/retail/mcp-server/src/index.ts"],
      "env": {
        "RETAIL_BACKEND_URL": "http://localhost:8080",
        "RETAIL_STORE_JWT": "你的店家JWT"
      }
    }
  }
}
```

生产或无 tsx 环境时：

```bash
cd mcp-server
npm install
npm run build
```

然后配置：

```json
{
  "mcpServers": {
    "retail-workbench": {
      "command": "node",
      "args": ["d:/retail/mcp-server/dist/index.js"],
      "env": {
        "RETAIL_BACKEND_URL": "http://localhost:8080",
        "RETAIL_STORE_JWT": "你的店家JWT"
      }
    }
  }
}
```

---

## 4. 扩展 MCP：新增“查库存”等工具（推荐做法）

你已经在后端有库存接口：

- `GET /api/store/products?name=xxx`（模糊查询，返回分页记录，含 `stock`）
- `PUT /api/store/products/{id}/stock`（修改库存）

要把它们暴露为 MCP 工具，建议在 `mcp-server/src/index.ts` 里新增：

1. `get_product_stock`（输入：name，输出：匹配商品列表及 stock）
2. `update_product_stock`（输入：id、stock，输出：ok/错误）

实现方式与 `fetchWorkbenchSummary()` 一致：用 `fetch()` 请求后端 REST，并在 headers 里带 `Authorization: Bearer ${RETAIL_STORE_JWT}`。

> 若你不想在 MCP server 中存 JWT，另一种方式是让后端对这些只读接口支持 `X-Internal-Api-Key` 放行（你已实现过该机制），然后 MCP server 使用内部 key 调后端。

---

## 5. 鉴权与安全（企业级建议）

在 MCP server 调后端时，常见两种鉴权：

### 5.1 店家 JWT（当前 mcp-server 就是这样）

- **优点**：数据天然带用户上下文，权限与审计跟业务一致
- **缺点**：JWT 过期需要更新；不建议长期写死在服务器环境变量里

### 5.2 内部 Key（`X-Internal-Api-Key`，推荐给“工具调用”）

- **优点**：稳定、不依赖登录态
- **缺点**：默认不带店家上下文（除非再设计一次性 token 或把 storeId 放入请求体并做校验）

你可以参考 `docs/DIFY_企业级回调鉴权.md` 的三种方式来统一工具调用鉴权。

---

## 6. 与 Dify 的关系（不要混淆）

- MCP 是给 **Cursor/IDE 的 AI** 用的工具协议
- Dify 的工具编排更常用 **HTTP 请求节点** 调你的后端 REST

两者可以并存：  
**线上**：Dify/Java 调 REST；**开发/运营**：Cursor 通过 MCP 调同一套 REST（或内部 Key）。

