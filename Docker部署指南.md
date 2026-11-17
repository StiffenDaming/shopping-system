# Docker 部署指南

> 使用 Docker Compose 一键部署整个项目（MySQL + 后端 + 前端/Nginx）
> 适用场景：生产部署、服务器迁移、演示环境

---

## 一、架构说明

```
                    ┌─────────────────────────────────┐
                    │        Docker 网络               │
                    │   (shopping-net)                │
                    │                                 │
   浏览器 ── 80 ──►  Nginx (frontend 容器)            │
                    │     │                           │
                    │     └── /api/ ──► 后端 8080     │
                    │                     │           │
                    │                     └──► MySQL  │
                    │                          3306   │
                    └─────────────────────────────────┘
```

| 容器 | 镜像 | 端口映射 | 说明 |
|------|------|---------|------|
| shopping-mysql | mysql:8.0 | 3307:3306 | MySQL 数据库，宿主机用 3307 访问 |
| shopping-backend | 自行构建 | 8081:8080 | SpringBoot 后端 |
| shopping-frontend | 自行构建 | 80:80 | Nginx + 前端静态文件 |

---

## 二、前置条件

- Docker 20.10+
- Docker Compose v2+
- 服务器内存 ≥ 2GB（构建阶段需要）

验证：
```bash
docker --version
docker compose version
```

---

## 三、一键部署

### 1. 上传项目到服务器

```bash
# 把 shopping-system 整个目录传到服务器
scp -r shopping-system user@server:/path/to/
```

或者用 Git：
```bash
git clone <你的仓库地址>
cd shopping-system
```

### 2. 启动所有服务

```bash
cd shopping-system
docker compose up -d
```

首次启动会自动构建镜像（需要下载依赖，比较慢，5-15 分钟）。

### 3. 查看启动状态

```bash
docker compose ps
```

三个容器状态都是 `Up` / `healthy` 就 OK 了。

### 4. 访问

浏览器打开 `http://服务器IP`（80 端口，直接访问）

默认账号：
- 管理员：`admin` / `123456`
- 普通用户：`user` / `123456`

---

## 四、常用命令

```bash
# 启动（后台运行）
docker compose up -d

# 停止
docker compose stop

# 停止并删除容器（数据还在 volume 里）
docker compose down

# 停止并删除所有（包括数据卷，慎用！）
docker compose down -v

# 查看日志
docker compose logs -f           # 所有服务
docker compose logs -f backend   # 只看后端
docker compose logs -f mysql     # 只看数据库
docker compose logs -f frontend  # 只看前端

# 进入容器
docker exec -it shopping-mysql mysql -uroot -p123456   # 进 MySQL
docker exec -it shopping-backend bash                  # 进后端容器

# 重启某个服务
docker compose restart backend

# 重新构建并启动（代码更新后）
docker compose up -d --build
```

---

## 五、推荐模块与大模型 Key

推荐模块的标签抽取依赖大模型 API，Key 通过环境变量注入，不写入镜像与配置文件。

在项目根目录建 `.env`（已被 `.gitignore` 忽略）后，`docker compose` 会自动读取，无需每次带前缀：

```bash
cp .env.example .env      # 然后编辑 .env，填入 LLM_API_KEY=sk-xxxx
docker compose up -d --build
```

未注入时系统正常启动，标签抽取自动降级为词典规则，其余功能不受影响。

确认容器内是否注入成功：

```bash
docker compose exec backend env | grep LLM_API_KEY
```

部署后可用以下接口确认状态（需管理员 Token）：

```bash
curl http://localhost:8081/api/admin/recommend/status -H "Authorization: Bearer <token>"
# {"llmEnabled":true,"productCount":19}
```

---

## 六、Navicat 连接 Docker 里的 MySQL

| 字段 | 值 |
|------|-----|
| 主机 | `localhost`（本地）或服务器 IP（远程） |
| 端口 | `3307`（注意不是 3306，映射到宿主机的是 3307） |
| 用户名 | `root` |
| 密码 | `123456` |

---

## 六、数据持久化

MySQL 数据存在 Docker volume 里（`shopping_mysql-data`），即使删除容器，数据也不会丢。

```bash
# 查看 volume
docker volume ls | grep shopping

# 备份数据库
docker exec shopping-mysql mysqldump -uroot -p123456 shopping > backup.sql

# 恢复数据库
docker exec -i shopping-mysql mysql -uroot -p123456 shopping < backup.sql
```

---

## 七、更新代码后重新部署

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建并启动（必须带 --build）
docker compose up -d --build

# 3. 只重建后端（前端没变的话更快）
docker compose up -d --build backend
```

> **注意**：`docker compose up -d` 不会重新构建镜像。不加 `--build`，容器里跑的还是旧镜像，
> 表现为「改了代码但页面/接口没变化」。

### 确认容器确实用的是新镜像

```bash
curl http://localhost/build-info.json
# 返回镜像内构建时间戳，例如 2026-11-17T09:05:38Z
```

时间戳停留在旧值 → 镜像没重建。
时间戳是新的但页面仍旧 → 浏览器缓存，`Ctrl + Shift + R` 硬刷新；详见
[docs/前端缓存与部署刷新笔记.md](docs/前端缓存与部署刷新笔记.md)。

---

## 八、配置说明

### 环境变量

后端容器支持的环境变量：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| SPRING_PROFILES_ACTIVE | docker | 使用的 profile |
| TZ | Asia/Shanghai | 时区 |
| LLM_API_KEY | 空 | 大模型 API Key，为空时标签抽取降级为规则匹配 |

如果要改数据库密码等，修改 `docker-compose.yml` 里的环境变量即可。

### 修改端口

编辑 `docker-compose.yml` 的 ports 部分：

```yaml
ports:
  - "8081:8080"   # 左边是宿主机端口，右边是容器端口
```

左边的数字可以随便改，右边的不要动。

---

## 九、常见问题

### Q1: 第一次启动后端连不上 MySQL

正常现象。MySQL 第一次启动需要初始化数据，后端启动快了就会连不上。docker-compose 里配置了 healthcheck 和 depends_on，会等 MySQL 就绪后再启动后端。

如果还是不行，手动重启后端：
```bash
docker compose restart backend
```

### Q2: 构建镜像慢（Maven/npm 下载依赖）

- 网络慢的话，可以配置 Docker 镜像加速器
- 或者在本地构建好镜像，推送到镜像仓库，服务器直接 pull

### Q3: 上传文件丢失

Docker 容器里的文件系统是临时的，重启容器会丢失。如果需要持久化上传的图片，需要加 volume 映射：

```yaml
backend:
  volumes:
    - ./uploads:/app/uploads
```

### Q4: 内存不够，构建失败

服务器内存不足 2GB 时，Maven 构建可能 OOM。解决方案：
- 加大服务器内存
- 或者本地构建好 jar 包，Dockerfile 直接复制 jar（多阶段构建的第二阶段）

---

### Q5: 构建失败，报 `invalid file request node_modules/.bin/esbuild`

构建上下文包含了宿主机的 `node_modules`，其中的符号链接在 WSL 路径上无法处理。
确认 `frontend/.dockerignore` 与 `backend/.dockerignore` 存在，且都包含 `node_modules` / `target`。
这两个文件必须提交到仓库。

### Q6: 构建失败，报 `npm ci` 找不到 package-lock.json

`frontend/Dockerfile` 使用 `npm ci` 构建，必须有 `package-lock.json`。
该文件同样必须提交到仓库，`.gitignore` 中已放行。

---

## 十、生产环境优化建议

1. **Nginx 配置 HTTPS**：申请 SSL 证书，配置 443 端口
2. **数据库密码**：改成强密码，不要用 123456
3. **JWT Secret**：生成新的密钥，不要用默认的
4. **日志管理**：配置日志滚动，避免日志文件过大
5. **备份策略**：定时备份 MySQL 数据到外部存储
6. **资源限制**：给每个容器加 CPU/内存限制
7. **反向代理**：前面加一层 Nginx 或 CDN，处理静态资源和负载均衡
