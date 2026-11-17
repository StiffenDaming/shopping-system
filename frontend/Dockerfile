# ===== 构建阶段 =====
FROM node:18-alpine AS build

WORKDIR /app

# 先复制 package.json，利用 Docker 缓存
COPY package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# 复制源码并构建
COPY . .
# 同时写入构建时间戳，用于确认容器里跑的到底是不是新镜像
RUN npm run build \
    && date -u +"%Y-%m-%dT%H:%M:%SZ" > /app/dist/build-info.json

# ===== 运行阶段 =====
FROM nginx:alpine

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
