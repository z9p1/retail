# Retail MCP Server

单独进程的 MCP 服务，暴露 **get_workbench_summary** 工具，请求零售后端的工作台汇总接口。  
在 Cursor 等支持 MCP 的客户端中配置后，可用自然语言让 AI 查询你当前店的工作台数据。

## 依赖

- Node.js 18+
- 零售后端已启动（默认 `http://localhost:8080`）
- 店家 JWT（需先在前端以店家身份登录获取）

## 安装与运行

```bash
cd mcp-server
npm install
```

开发时直接跑（stdio 会阻塞，用于被 Cursor 拉起）：

```bash
npm run dev
# 或
npx tsx src/index.ts
```

生产可先编译再跑：

```bash
npm run build
npm start
```

## 环境变量

| 变量 | 说明 | 默认 |
|------|------|------|
| `RETAIL_BACKEND_URL` | 零售后端根地址 | `http://localhost:8080` |
| `RETAIL_STORE_JWT` | 店家 JWT，用于请求 `/api/store/workbench` | 空（不传则后端通常 401） |

复制 `.env.example` 为 `.env` 并填入 `RETAIL_STORE_JWT`。获取方式：浏览器登录店家端 → F12 → Application → Local Storage → 找到存 token 的 key，或 Network 里看登录响应里的 token。

## 在 Cursor 中配置 MCP

1. 打开 Cursor 设置 → **MCP**（或 `cursor.json` / 用户配置中的 `mcpServers`）。
2. 添加一个 stdio 类型的 MCP 服务器，例如：

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

若已编译，可改为：

```json
"retail-workbench": {
  "command": "node",
  "args": ["d:/retail/mcp-server/dist/index.js"],
  "env": { ... }
}
```

3. 重启 Cursor 或重载 MCP，在对话中即可让 AI 使用「工作台汇总」工具（如：“查一下我店里的工作台数据”）。

## 暴露的工具

| 工具名 | 说明 |
|--------|------|
| `get_workbench_summary` | 获取工作台汇总：今日/本周订单数、销售额、待发货数、低库存数、零库存商品列表等。无参数。 |

数据来源：后端 `GET /api/store/workbench`（需店家 JWT）。
