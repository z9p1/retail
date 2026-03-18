# Dify 接入操作手册

本文档说明如何**使用 Docker 安装 Dify**，并在本零售项目中完成**智能助手接入 Dify** 的完整操作步骤。适合开发与运维按顺序执行。

---

## 目录

1. [概述与前置条件](#一概述与前置条件)
2. [Docker 安装 Dify（详细步骤）](#二docker-安装-dify详细步骤)
3. [在 Dify 中创建应用并获取 API](#三在-dify-中创建应用并获取-api)
4. [本项目配置为使用 Dify](#四本项目配置为使用-dify)
5. [验证与排错](#五验证与排错)
6. [可选：知识库与工作流](#六可选知识库与工作流)

---

## 一、概述与前置条件

### 1.1 接入方式说明

- **前端**：仍请求本项目的 `POST /api/store/agent/chat`，无需改前端代码。
- **后端**：当 `agent.provider=dify` 时，Java 将用户输入转发到 **Dify 的对话 API**（`/v1/chat-messages`），由 Dify 内的应用（工作流/Agent）负责推理；多轮会话的 `conversation_id` 由后端从 Redis 读写并透传给 Dify。
- **Dify**：负责 LLM 调用、可选知识库 RAG、工作流编排；若需「工作台/流量」等实时数据，在 Dify 工作流中通过 **HTTP 请求节点** 调用本后端接口。

### 1.2 前置条件

| 项目 | 要求 |
|------|------|
| Docker | 19.03+ |
| Docker Compose | V2（`docker compose`）或 V1（`docker-compose`） |
| 硬件 | 建议 CPU ≥ 2 核，内存 ≥ 4GB |
| 本机 | 已安装并启动 MySQL、Redis（本项目后端依赖）；Dify 与后端需能互相访问（同机可用 `localhost` 或 `host.docker.internal`） |

- **Windows**：建议使用 WSL2 + Docker Desktop，或 Docker Desktop 直接安装；数据目录建议放在 WSL 的 Linux 文件系统下。
- **Linux / macOS**：直接使用 Docker 与 Docker Compose 即可。

---

## 二、Docker 安装 Dify（详细步骤）

### 2.1 安装 Docker 与 Docker Compose

- **Windows / macOS**：安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)，其自带 Docker Compose。  
- **Linux**：  
  - Docker：`curl -fsSL https://get.docker.com | sh` 或按发行版文档安装。  
  - Docker Compose V2：通常随 Docker 插件安装（`docker compose version` 可验证）。

确认版本：

```bash
docker --version
docker compose version
# 或旧版：docker-compose --version
```

### 2.2 克隆 Dify 仓库并进入 docker 目录

```bash
git clone https://github.com/langgenius/dify.git
cd dify/docker
```

> 若网络受限，可使用镜像或代理；或从 [Dify  releases](https://github.com/langgenius/dify/releases) 下载源码包并解压后进入 `dify/docker`。

### 2.3 配置环境变量 .env

复制示例配置并编辑：

```bash
# Linux / macOS
cp .env.example .env

# Windows CMD
copy .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

用编辑器打开 `.env`，**本地部署**至少关注并可按需修改以下项：

| 变量 | 说明 | 本地建议值 |
|------|------|------------|
| `CONSOLE_API_URL` | 控制台 API 后端地址，用于回调等 | 留空或 `http://localhost` |
| `CONSOLE_WEB_URL` | 控制台前端地址，CORS 等 | 留空或 `http://localhost` |
| `SERVICE_API_URL` | 服务 API 基础 URL，前端展示用 | 留空或 `http://localhost` |
| `TRIGGER_URL` | 触发器/Webhook 回调基础 URL | `http://localhost`（示例中已有） |
| `SECRET_KEY` | 会话与敏感信息加密密钥 | 保持示例或使用 `openssl rand -base64 42` 生成 |
| `INIT_PASSWORD` | 首次创建管理员时的密码（可选） | 若设置则安装页可直接用该密码，不超过 30 字符 |

**本地访问**：若仅本机浏览器访问 Dify，上述 URL 类变量可保持为空或设为 `http://localhost`，Dify 会使用当前访问域名。

**生产/远程访问**：需将 `CONSOLE_API_URL`、`CONSOLE_WEB_URL`、`SERVICE_API_URL` 等改为实际访问地址（如 `https://dify.yourdomain.com`），否则登录回调、API 展示等可能异常。

### 2.4 启动所有服务

在 `dify/docker` 目录下执行：

**Docker Compose V2：**

```bash
docker compose up -d
```

**Docker Compose V1：**

```bash
docker-compose up -d
```

首次会拉取镜像，耗时会稍长。确认所有容器为 `running`：

```bash
docker compose ps
# 或：docker-compose ps
```

### 2.5 首次访问与初始化

1. 浏览器打开：**http://localhost/install**（若改过端口或域名，则用对应地址 + `/install`）。
2. 按页面提示设置**管理员账号**（邮箱 + 密码）；若在 `.env` 中配置了 `INIT_PASSWORD`，可能直接使用该密码完成初始化。
3. 初始化完成后访问：**http://localhost**，使用刚创建的账号登录。

### 2.6 端口与默认服务

Dify 的 `docker-compose` 会启动多类服务（API、Worker、Web、Nginx 等）。默认通过 **80 端口**对外提供 Web 与 API；若 80 被占用，可在 `docker-compose.yaml` 中修改端口映射（如 `8080:80`），然后通过 `http://localhost:8080` 访问。

### 2.7 常见问题

| 现象 | 处理建议 |
|------|----------|
| 端口 80 被占用 | 修改 `docker-compose.yaml` 中 nginx 的端口映射，或关闭占用 80 的进程。 |
| 容器反复重启 | 查看日志：`docker compose logs -f api`（或对应服务名）；检查内存是否 ≥ 4GB、`.env` 是否抄写错误。 |
| Windows 下权限或路径错误 | 将项目放在 WSL 的 Linux 文件系统中再执行 `docker compose`。 |
| 无法拉取镜像 | 配置 Docker 镜像加速或代理后重试 `docker compose up -d`。 |
| 登录后跳转异常 | 检查 `CONSOLE_WEB_URL` / `CONSOLE_API_URL` 是否与浏览器访问的域名一致（生产环境尤其注意 HTTPS）。 |

---

## 三、在 Dify 中创建应用并获取 API

### 3.1 创建应用

1. 登录 Dify 控制台后，进入「工作室」→「创建应用」。
2. 选择 **「对话型应用」**（或「工作流」按需编排）。
3. 在应用内配置 **模型**：在「模型」设置中选择可用的 LLM（如 OpenAI、通义、千问等，需在 Dify「设置」→「模型」中先配置好 API Key）。
4. （可选）若需助手能查「工作台、流量」等实时数据：
   - 在工作流中加入 **「HTTP 请求」** 节点。
   - URL 示例：`GET http://host.docker.internal:8080/api/store/workbench`、`GET http://host.docker.internal:8080/api/store/traffic?range=7`（其中 `8080` 为本项目后端端口；若 Dify 与后端同机，可用 `host.docker.internal` 或宿主机 IP）。
   - 若后端对 `/api/store/*` 做了 JWT 校验，可参考 [DIFY_INTEGRATION.md](./DIFY_INTEGRATION.md) 使用「内部 API Key」放行 Dify 调用。

### 3.2 发布并获取 API Key 与 Base URL

1. 在应用中点击 **「发布」**，使当前版本生效。
2. 进入 **「API 访问」**（或「API」）：
   - 复制 **API Key**。
   - 复制 **Base URL**（形如 `http://localhost/v1` 或 `https://api.dify.ai/v1`）。  
   本项目中，**Base URL** 即 `agent.api-url` 要填写的值；**API Key** 要写入数据库 `agent_config`。

---

## 四、本项目配置为使用 Dify

### 4.1 修改 application.yml

在 `retail-backend/src/main/resources/application.yml` 中设置：

```yaml
agent:
  provider: dify
  api-url: http://localhost/v1
  # model 在 Dify 应用内配置，此处可不写
  # 若 Dify 与后端不在同一台机器，将 localhost 改为 Dify 实际地址，例如：
  # api-url: http://192.168.1.100/v1
```

- `agent.api-url` 必须与 Dify「API 访问」中的 **Base URL** 一致（包含 `/v1`）。
- 若 Dify 通过 Nginx 暴露在 80 以外端口，例如 8080，则填：`http://localhost:8080/v1`。

### 4.2 在数据库中配置 API Key

本项目从表 `agent_config` 读取 `config_key='agent_api_key'` 的 `config_value` 作为 Dify 的 API Key。

**确保表存在**（若尚未建表可执行）：

```sql
-- 见 retail-backend/src/main/resources/agent_config_ddl.sql
CREATE TABLE IF NOT EXISTS agent_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value VARCHAR(512),
  update_time DATETIME
);
```

**插入或更新 Dify API Key**：

```sql
INSERT INTO agent_config (config_key, config_value, update_time)
VALUES ('agent_api_key', '这里替换为 Dify 应用的 API Key', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), update_time = NOW();
```

将 `这里替换为 Dify 应用的 API Key` 换成「API 访问」中复制的 API Key。

### 4.3 重启后端

重启零售后端应用，使 `application.yml` 与数据库配置生效。之后店家端对话会走 Dify。

---

## 五、验证与排错

### 5.1 基本验证

1. 店家端打开智能助手，发送一句短消息（例如「你好」）。
2. 若 Dify 与模型配置正确，应能收到 Dify 应用返回的回复。
3. 多轮对话：同一店家再次发送，应能延续上下文（后端会从 Redis 取 `conversation_id` 并传给 Dify）。

### 5.2 配置对应关系（排错用）

| 本项目 | Dify / 说明 |
|--------|-------------|
| `agent.provider=dify` | 使用 Dify 对话 API |
| `agent.api-url` | Dify「API 访问」中的 **Base URL**（如 `http://localhost/v1`） |
| `agent_config.config_key='agent_api_key'` 的 `config_value` | Dify「API 访问」中的 **API Key** |
| 请求格式 | 后端对 Dify 发送 `POST {api-url}/chat-messages`，Body：`query`、`user`、`conversation_id`（可选）、`response_mode=blocking` |

### 5.3 常见错误

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 提示「智能助手尚未配置」 | `agent.api-url` 或数据库 `agent_api_key` 为空 | 检查 yml 与 `agent_config` 表。 |
| 助手调用失败 / 网络错误 | 后端无法访问 Dify（域名、端口、防火墙） | 用本机浏览器或 curl 访问 `http://<api-url>/chat-messages` 的域名与端口；若 Dify 在 Docker 内，后端在宿主机用 `localhost` 即可。 |
| 401 / 鉴权失败 | API Key 错误或过期 | 在 Dify 应用「API 访问」重新复制 Key，更新 `agent_config.config_value`。 |
| 无回复或超时 | Dify 内模型未配置或模型服务不可用 | 在 Dify「设置」→「模型」中检查对应 LLM 的 API Key 与可用性。 |

---

## 六、可选：知识库与工作流

### 6.1 使用 Dify 知识库（RAG）

1. 在 Dify「知识库」中创建知识库，上传或同步文档（如商品说明、FAQ）。
2. 在对话型应用或工作流中，添加 **「知识库检索」** 节点并选择该知识库，再连接 LLM 节点。
3. 发布后，对话时会先检索知识库再生成回答；无需在本项目 Java 侧单独实现 RAG（若仍保留本项目的 `ragService.retrieve`，会作为额外上下文拼进 `query` 传给 Dify）。

### 6.2 Dify 工作流中调用本后端

若希望 Dify 内直接请求本项目的接口（如工作台、流量）：

- 在工作流中添加 **「HTTP 请求」** 节点，URL 填：`http://host.docker.internal:8080/api/store/workbench` 等（端口按你实际后端端口修改）。
- 若后端需要鉴权：可在后端增加「内部 API Key」校验，在 Dify 的 HTTP 请求头中带上该 Key，详见 [DIFY_INTEGRATION.md](./DIFY_INTEGRATION.md)。

### 6.3 生产环境与 DSL

- 工作流在**开发/本机**的 Dify 网页中编排后，可在应用内 **导出 DSL**（YAML），在生产 Dify 中 **导入 DSL** 复现同一工作流，无需在生产服务器上再次「画线」。详见 [DIFY_RAG_MILVUS_LLMOPS.md](./DIFY_RAG_MILVUS_LLMOPS.md) 第十二节。

---

## 附录：相关文档

- [DIFY_INTEGRATION.md](./DIFY_INTEGRATION.md)：两种接入方式（助手后端用 Dify / 仅用 Dify 做 RAG）及内部 Key 说明。  
- [DIFY_RAG_MILVUS_LLMOPS.md](./DIFY_RAG_MILVUS_LLMOPS.md)：整体架构、RAG、Milvus、Java 与 Dify 分工及生产部署要点。  
- Dify 官方安装文档：<https://docs.dify.ai/zh-hans/getting-started/install-self-hosted/docker-compose>（以官方最新文档为准）。
