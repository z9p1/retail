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
