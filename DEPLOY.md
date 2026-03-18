# 店家零售系统 - 阿里云打包与部署说明

## 一、本地打包

### 1. 后端（Spring Boot）

在项目根目录或 `retail-backend` 目录下执行：

```bash
cd retail-backend
mvn clean package -DskipTests
```

- 产物位置：`retail-backend/target/retail-backend-1.0.0.jar`
- 该 JAR 为可执行 fat jar，包含所有依赖，可直接用 `java -jar` 运行。

### 2. 前端（Vue + Vite）

```bash
cd retail-frontend
npm ci
npm run build
```

- 产物位置：`retail-frontend/dist/`（静态 HTML/JS/CSS）
- 部署时需通过 Nginx 或其它 Web 服务器提供该目录，并将 `/api` 反向代理到后端。

---

## 二、阿里云部署方式概览

| 方式 | 适用场景 | 说明 |
|------|----------|------|
| **ECS + 手动部署** | 快速上线、单机 | 上传 JAR + 前端 dist，用 Nginx 反向代理 |
| **Docker** | 环境一致、易迁移 | 使用项目内 Dockerfile 构建镜像，可部署到 ECS 或 ACK |
| **阿里云 RDS + Redis** | 生产推荐 | 数据库与缓存使用云产品，应用只部署 JAR |

---

## 三、方式一：ECS 直接部署（推荐入门）

### 3.1 准备

- 一台阿里云 ECS（建议 2 核 4G 起），系统选 CentOS 7/8 或 Alibaba Cloud Linux。
- 已购买/开通：**RDS MySQL**、**Redis 实例**（或在同一台 ECS 自建 MySQL/Redis）。

### 3.2 上传产物

将以下内容上传到 ECS（如 `/opt/retail`）：

- `retail-backend/target/retail-backend-1.0.0.jar`
- `retail-frontend/dist/` 整个目录（可重命名为 `dist` 或 `web`）

### 3.3 后端运行（使用生产配置）

```bash
# 使用生产配置，并通过环境变量指定 RDS、Redis 等
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:mysql://你的RDS地址:3306/retail?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai"
export SPRING_DATASOURCE_USERNAME=你的数据库用户名
export SPRING_DATASOURCE_PASSWORD=你的数据库密码
export SPRING_REDIS_HOST=你的Redis地址
export SPRING_REDIS_PORT=6379
# 如有 Redis 密码
export SPRING_REDIS_PASSWORD=你的Redis密码

java -jar -Xms256m -Xmx512m /opt/retail/retail-backend-1.0.0.jar
```

建议用 **systemd** 或 **supervisor** 做进程守护与开机自启。

### 3.4 Nginx 配置（前端 + 反向代理 /api）

安装 Nginx 后，新增站点配置（如 `/etc/nginx/conf.d/retail.conf`）：

```nginx
server {
    listen 80;
    server_name 你的域名或ECS公网IP;

    # 前端静态
    root /opt/retail/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

重载 Nginx：`nginx -s reload`。

### 3.4.1 改为 HTTPS 访问（推荐对外项目）

当前是 `http://101.132.24.6/login`，要改成 **https** 需要两步：**证书** + **Nginx 开 443**。

#### 第一步：准备 SSL 证书

| 情况 | 做法 |
|------|------|
| **有域名**（如 `retail.你的域名.com` 已解析到 101.132.24.6） | 用 **Let's Encrypt** 免费证书：`sudo certbot certonly --standalone -d retail.你的域名.com`，证书在 `/etc/letsencrypt/live/你的域名/`。 |
| **只有 IP**（如 101.132.24.6，无域名） | 用**自签名证书**：`sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/nginx/ssl/retail.key -out /etc/nginx/ssl/retail.crt`（浏览器会提示“不安全”，需手动信任或仅内网用）。 |

#### 第二步：Nginx 配置 HTTPS

在 `/etc/nginx/conf.d/retail.conf` 中改为（或新增）下面内容。**有域名**时把 `server_name` 和 `ssl_certificate` 路径换成你的；**只有 IP** 时用自签名路径，`server_name` 可写 `101.132.24.6`：

```nginx
# HTTP 跳转到 HTTPS（可选）
server {
    listen 80;
    server_name 101.132.24.6;   # 或你的域名
    return 301 https://$server_name$request_uri;
}

# HTTPS
server {
    listen 443 ssl;
    server_name 101.132.24.6;   # 或你的域名，如 retail.你的域名.com

    ssl_certificate     /etc/letsencrypt/live/你的域名/fullchain.pem;   # Let's Encrypt
    ssl_certificate_key /etc/letsencrypt/live/你的域名/privkey.pem;
    # 自签名则改为：/etc/nginx/ssl/retail.crt 和 /etc/nginx/ssl/retail.key

    root /opt/retail/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

执行：

```bash
sudo nginx -t && sudo nginx -s reload
```

云服务器需在**安全组/防火墙**放行 **443** 端口。完成后用 **https://101.132.24.6/login** 或 **https://你的域名/login** 访问即可。

### 3.5 数据库初始化

在 RDS 的 `retail` 库中执行项目中的 DDL/DML：

- 执行 `retail-backend/src/main/resources/ddl.sql`（如有）
- 执行 `retail-backend/src/main/resources/dml.sql`（如有）

确保与 `application-prod.yml` 中的库名、表结构一致。

---

## 四、方式二：Docker 部署（可选）

项目内已提供 Dockerfile，可用于在阿里云 ECS 或容器服务 ACK 上运行。

### 4.1 构建镜像

在 **项目根目录 `retail`** 下执行即可（镜像内会自动完成前端、后端构建，无需先本地打包）：

```bash
docker build -t retail-app:1.0 .
```

若前端没有 `package-lock.json`，请先在 `retail-frontend` 下执行一次 `npm install` 并提交该文件，否则 Docker 构建时 `npm ci` 可能失败。

### 4.2 运行容器

需传入生产环境变量（或使用 `-e`、`.env` 文件）：

```bash
docker run -d --name retail \
  -p 80:80 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://你的RDS:3306/retail?..." \
  -e SPRING_DATASOURCE_USERNAME=xxx \
  -e SPRING_DATASOURCE_PASSWORD=xxx \
  -e SPRING_REDIS_HOST=你的Redis地址 \
  -e SPRING_REDIS_PASSWORD=xxx \
  retail-app:1.0
```

前端已包含在同一镜像内，容器对外暴露 80 端口，访问 `http://服务器IP` 即可。

### 4.3 推送到阿里云镜像仓库（可选）

```bash
# 登录阿里云容器镜像服务
docker login --username=你的阿里云账号 registry.cn-hangzhou.aliyuncs.com

# 打标签并推送
docker tag retail-app:1.0 registry.cn-hangzhou.aliyuncs.com/你的命名空间/retail-app:1.0
docker push registry.cn-hangzhou.aliyuncs.com/你的命名空间/retail-app:1.0
```

之后可在 ACK 或 ECS 上拉取该镜像部署。

---

## 五、生产环境配置说明

- 后端生产配置：`retail-backend/src/main/resources/application-prod.yml`
- 通过 `--spring.profiles.active=prod` 或环境变量 `SPRING_PROFILES_ACTIVE=prod` 启用。
- 数据库、Redis、JWT 等均可通过环境变量覆盖，避免把密码写在配置文件里。

---

## 六、一键打包脚本（Windows PowerShell）

在项目根目录 `retail` 下可保存为 `pack.ps1`，方便本地一次性打出后端 JAR 与前端 dist：

```powershell
# pack.ps1
Set-Location $PSScriptRoot

Write-Host "Building backend..."
Set-Location retail-backend
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { exit 1 }
Set-Location ..

Write-Host "Building frontend..."
Set-Location retail-frontend
npm ci
npm run build
if ($LASTEXITCODE -ne 0) { exit 1 }
Set-Location ..

Write-Host "Done. Backend: retail-backend\target\retail-backend-1.0.0.jar"
Write-Host "Frontend: retail-frontend\dist\"
```

执行：`powershell -ExecutionPolicy Bypass -File pack.ps1`。

---

## 七、总结

| 步骤 | 命令/产物 |
|------|-----------|
| 打包后端 | `cd retail-backend && mvn clean package -DskipTests` → `target/retail-backend-1.0.0.jar` |
| 打包前端 | `cd retail-frontend && npm ci && npm run build` → `dist/` |
| 生产配置 | 使用 `application-prod.yml`，并配置 RDS、Redis 等环境变量 |
| 部署 | ECS：上传 JAR + dist，Nginx 反代 /api；或使用 Docker 镜像部署 |

按上述步骤即可将本系统打包并部署到阿里云。

---

## 八、Linux 部署 Dify 与 Milvus（在已有 Spring 部署基础上追加）

你已有一键部署脚本部署了 Spring 后端；下面在同一台或另一台 Linux 上部署 **Dify** 和 **Milvus**，与现有 JAR + Nginx 互不冲突。

### 8.1 前置条件

- 已安装 **Docker** 与 **Docker Compose**（V2 推荐：`docker compose version`）。
- 若与 Spring 同机：Dify 默认占 **80** 端口，若 Nginx 已占 80，需把 Dify 改为其它端口（见下）。
- 建议内存 ≥ 4GB（Dify 较吃内存）。

### 8.2 一键部署脚本（推荐）

项目内已提供脚本，上传到 Linux 后执行即可安装并启动 Dify + Milvus：

```bash
# 上传 deploy/deploy-dify-milvus.sh 到服务器后
chmod +x deploy/deploy-dify-milvus.sh
./deploy/deploy-dify-milvus.sh
```

脚本会：

1. 检查 Docker / Docker Compose。
2. **Dify**：在 `/opt/dify` 克隆 Dify 仓库，复制 `.env.example` 为 `.env`，执行 `docker compose up -d`（首次会拉镜像）。
3. **Milvus**：在 `/opt/milvus` 下载官方 standalone 的 docker-compose，执行 `docker compose up -d`，对外端口 **19530**。

执行完成后：

- **Dify**：浏览器访问 `http://服务器IP/install` 完成初始化（若改过端口则用 `http://服务器IP:端口/install`）。
- **Milvus**：供 Python RAG 或 Dify 连接，地址 `http://服务器IP:19530`（gRPC 默认 19530）。

### 8.3 手动部署步骤（可选）

**Dify**

```bash
sudo mkdir -p /opt/dify && cd /opt/dify
sudo git clone https://github.com/langgenius/dify.git .
cd docker
sudo cp .env.example .env
# 生产环境：编辑 .env，设置 CONSOLE_WEB_URL、CONSOLE_API_URL、SERVICE_API_URL 为实际域名或 IP
sudo docker compose up -d
```

若 80 端口已被 Nginx 占用，编辑 `docker/docker-compose.yaml` 中 nginx 的端口映射，例如改为 `8081:80`，则访问 `http://服务器IP:8081`。

**Milvus（standalone）**

```bash
sudo mkdir -p /opt/milvus && cd /opt/milvus
sudo wget -q https://github.com/milvus-io/milvus/releases/download/v2.6.0/milvus-standalone-docker-compose.yml -O docker-compose.yml
sudo docker compose up -d
```

验证：`docker compose ps`，Milvus 监听 19530。

### 8.4 与现有 Spring 的衔接

| 组件 | 说明 |
|------|------|
| **Spring 后端** | 保持原有部署方式（JAR + Nginx），无需改动。 |
| **application.yml** | `agent.api-url` 填 Dify 的 API 地址，如 `http://服务器IP/v1`（若 Dify 改过端口则 `http://服务器IP:8081/v1`）；`agent.dify-internal-key` 与 Dify 工作流 HTTP 节点里 `X-Internal-Api-Key` 一致。 |
| **Dify** | 工作流里 HTTP 回调本后端时，URL 填 Spring 所在地址，如 `http://同机内网IP:8080/api/store/workbench`，Header 带 `X-Internal-Api-Key`。 |
| **Milvus** | 当前项目 RAG 使用 MySQL `rag_chunk`；若后续改用自建 RAG + Milvus，由 Python 服务或 Dify 连接 `服务器IP:19530`。 |

### 8.5 生产注意

- **Dify**：生产环境务必在 `.env` 中设置 `CONSOLE_WEB_URL`、`CONSOLE_API_URL`、`SERVICE_API_URL` 为实际访问地址（如 `https://dify.你的域名.com`），否则登录与 API 展示可能异常。详见 [docs/DIFY_接入操作手册.md](docs/DIFY_接入操作手册.md)。
- **工作流**：在开发/本机 Dify 编排好后导出 DSL，在生产 Dify 中导入即可，无需在服务器上画线。见 [docs/DIFY_RAG_MILVUS_LLMOPS.md](docs/DIFY_RAG_MILVUS_LLMOPS.md)。
