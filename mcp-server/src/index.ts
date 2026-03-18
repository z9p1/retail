/**
 * MCP Server: 暴露 get_workbench_summary，请求零售后端工作台汇总接口
 * 运行方式：npx tsx src/index.ts 或 npm run dev
 * Cursor 中配置：stdio 命令指向 node dist/index.js 或 npx tsx src/index.ts
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio";
import { z } from "zod";

const BACKEND_URL = process.env.RETAIL_BACKEND_URL || "http://localhost:8080";
const STORE_JWT = process.env.RETAIL_STORE_JWT || "";

async function fetchWorkbenchSummary(): Promise<string> {
  const url = `${BACKEND_URL.replace(/\/$/, "")}/api/store/workbench`;
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (STORE_JWT) {
    headers["Authorization"] = `Bearer ${STORE_JWT}`;
  }
  const res = await fetch(url, { headers });
  if (!res.ok) {
    const text = await res.text();
    return JSON.stringify({
      error: true,
      message: `后端返回 ${res.status}`,
      detail: text.slice(0, 500),
    });
  }
  const data = await res.json();
  return JSON.stringify(data, null, 2);
}

async function main() {
  const server = new McpServer({
    name: "retail-mcp-server",
    version: "1.0.0",
  });

  server.registerTool(
    "get_workbench_summary",
    {
      title: "工作台汇总",
      description:
        "获取店家线上零售系统工作台汇总：今日/本周订单数、销售额、待发货数、低库存数、零库存商品列表等。无需参数。",
      inputSchema: z.object({}),
    },
    async (): Promise<{ content: Array<{ type: "text"; text: string }> }> => {
      try {
        const text = await fetchWorkbenchSummary();
        return {
          content: [{ type: "text", text }],
        };
      } catch (e) {
        const message = e instanceof Error ? e.message : String(e);
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify({
                error: true,
                message: "请求失败",
                detail: message,
              }),
            },
          ],
        };
      }
    }
  );

  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
