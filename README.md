# 购物商城系统（Graduation Project）

基于 SpringBoot + Vue3 的购物商城系统，仿 saucedemo.com，支持用户端和管理端双角色，含完整 CRUD 操作。

## 项目概述

- **项目名称**: 购物商城系统
- **技术栈**: Java 11 + SpringBoot 2.7 + MyBatis-Plus + JWT + Vue3 + Vite + Element Plus
- **数据库**: MySQL 8.0（Docker 部署 / 本地安装均可）
- **角色**: 管理员（ADMIN）+ 普通用户（USER）
- **功能模块**: 商品浏览、购物车、下单支付、订单管理、用户管理、商品管理、数据统计

## 项目结构

```
shopping-system/
├── backend/                    # SpringBoot 后端
│   ├── Dockerfile              # 后端 Docker 构建
│   ├── pom.xml                 # Maven 依赖配置
│   ├── src/main/java/com/demo/shopping/
│   │   ├── ShoppingApplication.java    # 启动类
│   │   ├── common/              # 公共模块（Result、JWT、异常处理）
│   │   ├── config/              # 配置类（MyBatis-Plus、Web、数据初始化）
│   │   ├── controller/          # 控制器层（7个）
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 实体类（6个）
│   │   ├── interceptor/         # JWT 拦截器
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   └── service/             # 业务逻辑层
│   └── src/main/resources/
│       ├── application.yml      # 应用配置（mysql / docker 双 Profile）
│       └── schema-mysql.sql     # MySQL 建表脚本
├── frontend/                    # Vue3 前端
│   ├── Dockerfile               # 前端 Docker 构建
│   ├── nginx.conf               # Nginx 配置（Docker 部署用）
│   ├── package.json
│   ├── vite.config.js           # Vite 配置（含代理）
│   └── src/
│       ├── main.js              # 入口文件
│       ├── App.vue
│       ├── router/              # 路由配置
│       ├── stores/              # Pinia 状态管理
│       ├── utils/               # 工具函数（Axios 封装）
│       ├── components/          # 公共组件
│       └── views/               # 页面
│           ├── Login.vue        # 登录页
│           ├── Register.vue     # 注册页
│           ├── Products.vue     # 商品列表（用户端）
│           ├── ProductDetail.vue # 商品详情
│           ├── Cart.vue         # 购物车
│           ├── Checkout.vue     # 结算页
│           ├── Orders.vue       # 我的订单
│           └── admin/           # 管理端页面
│               ├── AdminLayout.vue
│               ├── Dashboard.vue       # 数据看板
│               ├── UserManage.vue      # 用户管理
│               ├── ProductManage.vue   # 商品管理
│               └── OrderManage.vue     # 订单管理
├── docker-compose.yml           # Docker 一键编排（MySQL + 后端 + 前端）
├── maven-settings.xml           # Maven 镜像配置
├── Docker部署指南.md             # Docker 部署完整文档
└── 从零启动指南.md               # 本地开发启动指南
```

## 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 11 或更高 | SpringBoot 2.7 要求 Java 8+，推荐 17 |
| Maven | 3.6+ | 项目构建工具 |
| Node.js | 16+ | 前端开发，推荐 18 |
| npm | 8+ | 随 Node.js 自带 |
| MySQL | 8.0+ | 数据库，推荐 Docker 方式部署 |
| Docker | 20.10+ | 可选，推荐用于部署 MySQL |

## 快速启动

### 推荐方式：Docker MySQL + 本地开发

MySQL 跑在 Docker 里，前后端本地跑（有热更新）。

**1. 启动 Docker MySQL**
```bash
docker compose up -d mysql
```

**2. 启动后端**
```bash
cd backend
mvn spring-boot:run -s ../maven-settings.xml
```

**3. 启动前端**
```bash
cd frontend
npm install
npm run dev
```

**4. 访问**: http://localhost:5173

### 演示部署：Docker 全套

前后端 + 数据库全部 Docker 化，一条命令启动。

```bash
docker compose up -d
```

访问: http://localhost（80 端口）

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 普通用户 | user | 123456 |

## 功能清单

### 用户端功能
- [x] 用户注册 / 登录 / 退出
- [x] 商品列表浏览（搜索、分类筛选、排序、分页）
- [x] 商品详情查看
- [x] 购物车管理（加入、修改数量、删除）
- [x] 收货地址管理（增删改查、设为默认、结算时选择）
- [x] 提交订单（结算，可选择已保存地址）
- [x] 我的订单（查看、取消待发货、确认收货、红点提示）

### 管理端功能
- [x] 数据看板（用户/商品/订单统计、图表展示）
- [x] 用户管理（列表、角色切换、禁用/启用、重置密码）
- [x] 商品管理（新增、编辑、删除、上下架、图片、分类）
- [x] 订单管理（列表、详情、发货、强制完成、状态筛选）
- [x] 分类管理（列表、增删改）

## API 接口清单

### 认证接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/auth/login | 登录 | 公开 |
| POST | /api/auth/register | 注册 | 公开 |
| GET | /api/auth/info | 获取当前用户信息 | 登录 |
| PUT | /api/auth/password | 修改密码 | 登录 |
| PUT | /api/auth/profile | 更新个人资料 | 登录 |

### 商品接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/products/list | 商品分页列表 | 公开 |
| GET | /api/products/detail/{id} | 商品详情 | 公开 |
| GET | /api/products/admin | 管理员商品列表 | 管理员 |
| POST | /api/products | 新增商品 | 管理员 |
| PUT | /api/products | 修改商品 | 管理员 |
| PUT | /api/products/{id}/status | 上下架 | 管理员 |
| DELETE | /api/products/{id} | 删除商品 | 管理员 |

### 分类接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/categories/list | 分类列表 | 公开 |
| POST | /api/categories | 新增分类 | 管理员 |
| PUT | /api/categories | 修改分类 | 管理员 |
| DELETE | /api/categories/{id} | 删除分类 | 管理员 |

### 购物车接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/cart | 购物车列表 | 登录 |
| GET | /api/cart/count | 购物车商品数 | 登录 |
| POST | /api/cart | 加入购物车 | 登录 |
| PUT | /api/cart/{id} | 修改数量 | 登录 |
| DELETE | /api/cart/{id} | 删除购物车项 | 登录 |

### 订单接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/orders/checkout | 提交订单 | 登录 |
| GET | /api/orders | 我的订单列表 | 登录 |
| GET | /api/orders/{id} | 订单详情 | 登录 |
| PUT | /api/orders/{id}/cancel | 取消订单（带事务） | 登录 |
| PUT | /api/orders/{id}/confirm | 确认收货 | 登录 |
| GET | /api/orders/unread-count | 获取未读订单数（红点提示） | 登录 |
| PUT | /api/orders/mark-read | 标记订单已查看 | 登录 |
| GET | /api/orders/admin | 管理员订单列表 | 管理员 |
| GET | /api/orders/admin/{id} | 管理员订单详情 | 管理员 |
| PUT | /api/orders/admin/{id}/ship | 管理员发货 | 管理员 |
| PUT | /api/orders/admin/{id}/complete | 管理员强制完成 | 管理员 |

### 收货地址接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/addresses | 地址列表 | 登录 |
| GET | /api/addresses/default | 默认地址 | 登录 |
| POST | /api/addresses | 新增地址 | 登录 |
| PUT | /api/addresses/{id} | 修改地址 | 登录 |
| DELETE | /api/addresses/{id} | 删除地址 | 登录 |
| PUT | /api/addresses/{id}/default | 设为默认 | 登录 |

### 管理员接口
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/admin/stats | 统计数据 | 管理员 |
| GET | /api/admin/users | 用户列表 | 管理员 |
| PUT | /api/admin/users/{id}/role | 修改角色 | 管理员 |
| PUT | /api/admin/users/{id}/status | 修改状态 | 管理员 |
| PUT | /api/admin/users/{id}/reset-password | 重置密码 | 管理员 |

### 文件上传
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/upload | 上传图片 | 登录 |

## 数据库表结构

### sys_user（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(100) | 密码（BCrypt加密） |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像URL |
| role | VARCHAR(20) | 角色: USER/ADMIN |
| status | INT | 状态: 1-正常, 0-禁用 |
| last_view_orders_time | DATETIME | 上次查看订单时间（红点提示用） |
| deleted | INT | 逻辑删除: 0-未删, 1-已删 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### category（分类表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 分类ID |
| name | VARCHAR(50) | 分类名称 |
| sort | INT | 排序 |
| create_time | DATETIME | 创建时间 |

### product（商品表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 商品ID |
| name | VARCHAR(100) | 商品名称 |
| description | TEXT | 商品描述 |
| price | DECIMAL(10,2) | 价格 |
| image_url | VARCHAR(255) | 商品图片URL |
| category_id | BIGINT | 分类ID |
| stock | INT | 库存 |
| status | INT | 状态: 1-上架, 0-下架 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### cart_item（购物车表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 购物车项ID |
| user_id | BIGINT | 用户ID |
| product_id | BIGINT | 商品ID |
| quantity | INT | 数量 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### orders（订单表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 订单ID |
| order_no | VARCHAR(32) | 订单编号（唯一） |
| user_id | BIGINT | 用户ID |
| total_amount | DECIMAL(10,2) | 总金额 |
| status | VARCHAR(20) | 状态: PENDING/SHIPPED/COMPLETED/CANCELLED |
| receiver_name | VARCHAR(50) | 收货人 |
| receiver_phone | VARCHAR(20) | 联系电话 |
| receiver_address | VARCHAR(255) | 收货地址 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### order_item（订单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 明细ID |
| order_id | BIGINT | 订单ID |
| product_id | BIGINT | 商品ID |
| product_name | VARCHAR(100) | 商品名称（快照） |
| product_price | DECIMAL(10,2) | 商品单价（快照） |
| product_image | VARCHAR(255) | 商品图片（快照） |
| quantity | INT | 数量 |
| create_time | DATETIME | 创建时间 |

### delivery_address（收货地址表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 地址ID（主键） |
| user_id | BIGINT | 用户ID |
| receiver_name | VARCHAR(50) | 收货人姓名 |
| receiver_phone | VARCHAR(20) | 联系电话 |
| receiver_address | VARCHAR(255) | 详细地址 |
| is_default | INT | 是否默认: 0-否, 1-是 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 技术架构说明

### 后端架构
- **分层架构**: Controller → Service → Mapper
- **ORM**: MyBatis-Plus（简化 CRUD 操作，支持分页、逻辑删除）
- **认证**: JWT（JSON Web Token）+ 拦截器
- **密码加密**: BCrypt（Spring Security Crypto）
- **统一响应**: Result<T> 包装（code, message, data）
- **全局异常处理**: GlobalExceptionHandler
- **自动填充**: MyMetaObjectHandler（createTime, updateTime）

### 前端架构
- **框架**: Vue 3 (Composition API) + Vite
- **UI 库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4（带导航守卫）
- **HTTP**: Axios（封装请求/响应拦截器）
- **图表**: ECharts 5

## 核心业务流程

### 用户下单流程
1. 用户浏览商品 → 加入购物车
2. 进入购物车 → 确认商品和数量
3. 点击结算 → 选择已保存地址或手动填写收货信息 → 提交订单
4. 系统校验库存 → 创建订单 → 创建订单明细 → 扣减库存 → 清空购物车
5. 用户可在「我的订单」中查看订单，待发货状态可取消，已发货状态可确认收货

### 管理员处理订单流程
1. 管理员登录 → 订单管理
2. 查看待发货订单 → 点击「发货」→ 状态变为已发货（触发用户端红点提示）
3. 用户在「我的订单」确认收货 → 状态变为已完成
4. 管理员可对已发货订单点击「强制完成」→ 状态变为已完成（防止用户忘记确认）

### 订单状态流转
```
PENDING（待发货）
  ├── 用户取消 → CANCELLED（已取消，恢复库存）
  └── 管理员发货 → SHIPPED（已发货，触发红点）
       ├── 用户确认收货 → COMPLETED（已完成）
       └── 管理员强制完成 → COMPLETED（已完成）
```

## 初始数据

系统首次启动时自动初始化以下数据：

- **用户**: admin (管理员), user (普通用户) — 密码均为 123456
- **分类**: 服装、电子产品、图书、食品、运动户外 (共 5 个)
- **商品**: 14 件（含 Sauce Labs 系列 6 件 + 中文商品 8 件）

## 部署建议（生产环境）

推荐使用 Docker Compose 一键部署，详见 [Docker部署指南.md](Docker部署指南.md)。

手动部署方式：
1. 后端打包: `mvn clean package` → 生成 `target/shopping-1.0.0.jar`
2. 前端打包: `npm run build` → 生成 `dist/` 目录
3. 后端部署: `java -jar shopping-1.0.0.jar --spring.profiles.active=mysql`
4. 前端部署: 将 `dist/` 放到 Nginx 静态目录，配置反向代理到后端
5. 使用 Nginx 统一端口，配置 HTTPS

## 常见问题

### 1. Maven 下载依赖很慢 / 失败

项目已提供 `maven-settings.xml`，使用阿里云镜像：

```bash
mvn spring-boot:run -s ../maven-settings.xml
```

### 2. 端口被占用

```bash
# 查找占用进程
lsof -i:8080

# 杀掉进程
kill -9 <进程ID>
```

或修改 `application.yml` 中的端口号。

### 3. 前端 npm install 慢

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

### 4. 前端访问后端跨域问题

前端已通过 Vite 代理解决跨域（`vite.config.js` 中配置了 `/api` 代理）。如果后端端口有变化，需同步修改。

### 5. 登录失败 / 401 错误

- 确认用户名密码正确（admin/123456 或 user/123456）
- 确认后端服务正常运行
- 查看浏览器控制台和 Network 面板，检查请求详情

## 更多文档

- [从零启动指南.md](从零启动指南.md) — 本地开发完整启动步骤
- [Docker部署指南.md](Docker部署指南.md) — Docker 部署完整说明
