# 多阶段构建：店家零售系统 - 阿里云容器镜像
# 用法：先在本机执行前端 build + 后端 mvn package，再 docker build -t retail-app:1.0 .

# ---------- 阶段1：前端构建 ----------
FROM node:18-alpine AS frontend
WORKDIR /app
COPY retail-frontend/package.json retail-frontend/package-lock.json* ./
RUN npm ci --ignore-scripts
COPY retail-frontend/ .
RUN npm run build

# ---------- 阶段2：后端构建 ----------
FROM maven:3.8-eclipse-temurin-8-alpine AS backend
WORKDIR /app
COPY retail-backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY retail-backend/ .
RUN mvn clean package -DskipTests -B

# ---------- 阶段3：运行镜像 ----------
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

# 安装 Nginx
RUN apk add --no-cache nginx

# 前端静态
COPY --from=frontend /app/dist /app/web

# 后端 JAR
COPY --from=backend /app/target/retail-backend-*.jar /app/retail-backend.jar

# Nginx 配置：静态 + /api 反代到后端
RUN echo 'server { \
    listen 80; \
    root /app/web; \
    index index.html; \
    location / { try_files $uri $uri/ /index.html; } \
    location /api { proxy_pass http://127.0.0.1:8080; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; proxy_set_header X-Forwarded-Proto $scheme; } \
}' > /etc/nginx/http.d/default.conf

# 启动：先启后端，再前台运行 Nginx
RUN echo '#!/bin/sh' > /app/start.sh && \
    echo 'java -jar /app/retail-backend.jar &' >> /app/start.sh && \
    echo 'sleep 5' >> /app/start.sh && \
    echo 'exec nginx -g "daemon off;"' >> /app/start.sh && \
    chmod +x /app/start.sh

EXPOSE 80
CMD ["/app/start.sh"]
