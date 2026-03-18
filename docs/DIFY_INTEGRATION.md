# 本项目如何引进 Dify

[Dify](https://dify.ai) 是开源的 LLM 应用开发平台，支持可视化编排对话、Agent、RAG 等。下面给出两种常见接入方式，按需选用。

---

## 方式一：智能助手后端改用 Dify（推荐）

**思路**：前端仍请求本项目的 `POST /api/store/agent/chat`，后端改为调用 **Dify 的对话 API**（`/v1/chat-messages`），由 Dify 里的应用（Agent/工作流）负责推理；Dify 工作流中通过 **HTTP 请求节点** 调用本后端的工作台、流量接口获取实时数据。

### 1. 在 Dify 里创建应用

1. 部署或使用已有 Dify（本地 / 云）。
2. 创建 **对话型应用**（Agent 或 工作流）：
   - **Agent**：在「工具」里添加「自定义 API」或「HTTP 请求」，配置调用本后端。
   - **工作流**：在画布里加「LLM」节点 + 「HTTP 请求」节点，按需编排。

3. 配置 **HTTP 请求** 调用本后端：
   - 工作台汇总：`GET {零售后端地址}/api/store/workbench`  
   - 流量汇总：`GET {零售后端地址}/api/traffic?range=7`  
   - 若后端对 `/api/store/*` 做了 JWT 校验，有两种做法：
     - **A**：在本后端增加「内部 API Key」校验（见下方「后端：支持 Dify 调用」），Dify 请求头里带该 Key，后端放行。
     - **B**：在 Dify 变量里配置一个固定的店家 JWT，HTTP 请求头里带 `Authorization: Bearer {变量}`（需注意 JWT 过期后要更新）。

4. 在 Dify 应用「发布」→「API 访问」中拿到 **API Key** 和 **Base URL**（如 `https://api.dify.ai/v1` 或自建 `http://your-dify/v1`）。

### 2. 本后端配置为使用 Dify

在 `application.yml` 中（或环境变量）增加/修改：

```yaml
agent:
  provider: dify                    # 使用 Dify 对话接口
  api-url: https://api.dify.ai/v1   # Dify 应用 API 的 base URL（或自建地址）
  # model 在 Dify 端配置，此处可不写
```

`agent_api_key` 仍存在表 `agent_config` 中，但这里填的是 **Dify 应用的 API Key**（不是 OpenAI Key）。  
后端会向 `{api-url}/chat-messages` 发请求，把用户问题交给 Dify，并解析返回的 `answer` 返回给前端。

### 3. 后端：支持 Dify 调用本后端（已实现）

Dify 工作流用「内部 Key」调用本后端时**不需要店家 JWT**，已按企业级做法实现：

- 在 `application.yml` 中配置：`agent.dify-internal-key: 一串随机密钥`（不配置则不放行）。
- 对 `GET /api/store/workbench`、`GET /api/traffic`、`GET /api/traffic/trend` 等：若请求头 **X-Internal-Api-Key** 等于该配置，则放行（不校验 JWT）。
- 在 Dify 的 HTTP 请求节点里，Header 增加：`X-Internal-Api-Key: 与配置相同的密钥`。

详见 [DIFY_企业级回调鉴权.md](DIFY_企业级回调鉴权.md)。

---

## 方式二：仅用 Dify 做 RAG / 知识库

**思路**：智能助手仍用当前「OpenAI 兼容 API + 本后端工具」；仅把 **RAG 检索** 换成 Dify 的知识库能力。

1. 在 Dify 创建 **知识库**，上传/同步商品说明、FAQ 等文档。
2. 在 Dify 中通过「文档检索」API 或 在 Agent 里接入该知识库，得到检索结果。
3. 在本后端：
   - 要么：调用 Dify 的检索/对话 API 拿到「带知识库上下文的回答」，再与现有工具结果组合；
   - 要么：仅调用 Dify 的检索 API，把检索到的片段作为 RAG 上下文注入现有 `StoreAgentService` 的 system prompt（替代或补充当前自建的 `rag_chunk`）。

本方式不改动现有「工作台/流量」工具逻辑，只替换或增加 RAG 数据源。

---

## 小结

| 方式 | 适用场景 | 改动点 |
|------|----------|--------|
| **一：助手后端用 Dify** | 希望用 Dify 编排对话、工具、多步推理 | 后端改为调 Dify chat-messages；Dify 工作流里 HTTP 调本后端 workbench/traffic；可选加内部 Key 鉴权。 |
| **二：仅用 Dify 做 RAG** | 只想用 Dify 知识库，助手逻辑保留 | 后端 RAG 部分改为调 Dify 检索/知识库 API，或组合 Dify 返回的上下文。 |

按需选一种或组合使用即可。若你确定采用方式一，可让后端增加 `agent.provider=dify` 的分支和上述内部 Key 校验逻辑（需要的话我可以按你当前仓库结构写出具体改法）。
