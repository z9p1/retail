#!/usr/bin/env bash
# ============================================================
# Linux 一键部署 Dify + Milvus（与现有 Spring 部署无关，可同机或另机）
# 用法：chmod +x deploy-dify-milvus.sh && ./deploy-dify-milvus.sh
# ============================================================
set -e

DIFY_DIR="${DIFY_DIR:-/opt/dify}"
MILVUS_DIR="${MILVUS_DIR:-/opt/milvus}"
MILVUS_RELEASE="${MILVUS_RELEASE:-v2.6.0}"

echo "=== 检查 Docker ==="
if ! command -v docker &>/dev/null; then
  echo "未检测到 Docker，请先安装：curl -fsSL https://get.docker.com | sh"
  exit 1
fi
if ! docker compose version &>/dev/null && ! docker-compose version &>/dev/null; then
  echo "未检测到 Docker Compose，请先安装。"
  exit 1
fi
COMPOSE_CMD="docker compose"
if ! docker compose version &>/dev/null; then
  COMPOSE_CMD="docker-compose"
fi
echo "使用: $COMPOSE_CMD"

echo ""
echo "=== 部署 Dify ==="
if [ ! -d "$DIFY_DIR/docker" ]; then
  echo "克隆 Dify 到 $DIFY_DIR ..."
  sudo mkdir -p "$DIFY_DIR"
  sudo git clone --depth 1 https://github.com/langgenius/dify.git "$DIFY_DIR" || {
    echo "克隆失败，若网络受限可手动下载 https://github.com/langgenius/dify/releases 解压到 $DIFY_DIR"
    exit 1
  }
else
  echo "Dify 目录已存在，跳过克隆。"
fi

if [ ! -f "$DIFY_DIR/docker/.env" ]; then
  sudo cp "$DIFY_DIR/docker/.env.example" "$DIFY_DIR/docker/.env"
  echo "已生成 $DIFY_DIR/docker/.env，生产环境请编辑 CONSOLE_WEB_URL / CONSOLE_API_URL / SERVICE_API_URL"
else
  echo "Dify .env 已存在，跳过。"
fi

echo "启动 Dify 容器..."
cd "$DIFY_DIR/docker"
sudo $COMPOSE_CMD up -d
echo "Dify 已启动。首次访问: http://本机IP/install 完成初始化（若 80 被占用请改 docker-compose 中 nginx 端口）"

echo ""
echo "=== 部署 Milvus（standalone）==="
sudo mkdir -p "$MILVUS_DIR"
cd "$MILVUS_DIR"
if [ ! -f docker-compose.yml ]; then
  echo "下载 Milvus standalone docker-compose ..."
  sudo wget -q "https://github.com/milvus-io/milvus/releases/download/${MILVUS_RELEASE}/milvus-standalone-docker-compose.yml" -O docker-compose.yml || {
    echo "下载失败，可手动从 https://github.com/milvus-io/milvus/releases 下载 milvus-standalone-docker-compose.yml 放到 $MILVUS_DIR"
    exit 1
  }
fi
echo "启动 Milvus 容器..."
sudo $COMPOSE_CMD up -d
echo "Milvus 已启动，端口 19530（gRPC）。"

echo ""
echo "=== 完成 ==="
echo "Dify: 浏览器访问 http://本机IP/install 初始化后使用；API 一般为 http://本机IP/v1"
echo "Milvus: 连接地址 本机IP:19530"
echo "Spring 后端 agent.api-url 填 Dify 的 /v1 地址；Dify 工作流回调本后端时 URL 填 Spring 地址并带 X-Internal-Api-Key。"
