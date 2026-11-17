-- =============================================
-- 收藏品商城系统 建表脚本（MySQL 专用）
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `email` VARCHAR(100) COMMENT '邮箱',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER-普通用户, ADMIN-管理员',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
  `token_version` INT NOT NULL DEFAULT 1 COMMENT '令牌版本：改密/禁用时+1，使旧 token 失效',
  `last_view_orders_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上次查看订单列表的时间（用于红点提示）',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `description` TEXT COMMENT '商品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `image_url` VARCHAR(255) COMMENT '商品图片URL',
  `category_id` BIGINT COMMENT '分类ID',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 购物车表
CREATE TABLE IF NOT EXISTS `cart_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待发货, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消',
  `receiver_name` VARCHAR(50) COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) COMMENT '收货人电话',
  `receiver_address` VARCHAR(255) COMMENT '收货地址',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称(快照)',
  `product_price` DECIMAL(10,2) NOT NULL COMMENT '商品单价(快照)',
  `product_image` VARCHAR(255) COMMENT '商品图片(快照)',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 收货地址表
CREATE TABLE IF NOT EXISTS `delivery_address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（关联sys_user）',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `receiver_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default` INT NOT NULL DEFAULT 0 COMMENT '是否默认地址: 0-否, 1-是',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 首页轮播图表
CREATE TABLE IF NOT EXISTS `banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址（16:9）',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '标题（仅后台识别用）',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图表';

-- =============================================
-- 智能推荐模块（用户画像 + 热门推荐 + 过滤推荐）
-- =============================================

-- 用户行为日志表：推荐算法的数据地基
CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行为ID',
  `user_id` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID（0=游客）',
  `product_id` BIGINT DEFAULT NULL COMMENT '商品ID',
  `behavior_type` VARCHAR(20) NOT NULL COMMENT '行为类型: VIEW-浏览, CART-加购, ORDER-下单, SEARCH-搜索',
  `weight` DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '行为基础权重',
  `stay_ms` INT NOT NULL DEFAULT 0 COMMENT '页面停留时长(毫秒)',
  `keyword` VARCHAR(100) DEFAULT NULL COMMENT '搜索关键词(SEARCH时)',
  `scene` VARCHAR(50) DEFAULT NULL COMMENT '来源场景: LIST/DETAIL/CART/SEARCH',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_time` (`user_id`, `create_time`),
  INDEX `idx_product_time` (`product_id`, `create_time`),
  INDEX `idx_type_time` (`behavior_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为日志表';

-- 商品标签表：由大模型从商品描述中抽取的结构化特征
CREATE TABLE IF NOT EXISTS `product_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名',
  `tag_type` VARCHAR(20) NOT NULL DEFAULT 'GENERAL' COMMENT '标签类型: SUBJECT-题材, MATERIAL-材质, STYLE-风格, ERA-年代, SCENE-场景',
  `weight` DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '标签权重',
  `source` VARCHAR(20) NOT NULL DEFAULT 'LLM' COMMENT '来源: LLM-大模型, MANUAL-人工, RULE-规则',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_tag` (`product_id`, `tag_name`),
  INDEX `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签表';

-- 用户画像表：由行为日志聚合出的标签偏好向量
CREATE TABLE IF NOT EXISTS `user_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '画像ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `tag_vector` TEXT COMMENT '标签偏好向量 JSON: {"tag":weight}',
  `category_vector` TEXT COMMENT '分类偏好向量 JSON: {"categoryId":weight}',
  `price_pref` DECIMAL(10,2) DEFAULT NULL COMMENT '价格偏好(加权均价)',
  `activity_level` VARCHAR(20) NOT NULL DEFAULT 'LOW' COMMENT '活跃度: HIGH/MID/LOW',
  `view_cnt` INT NOT NULL DEFAULT 0 COMMENT '累计浏览次数',
  `cart_cnt` INT NOT NULL DEFAULT 0 COMMENT '累计加购次数',
  `order_cnt` INT NOT NULL DEFAULT 0 COMMENT '累计下单次数',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

-- 商品热度表：按天统计的加权热度分
CREATE TABLE IF NOT EXISTS `product_hot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '热度ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `hot_score` DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '热度分',
  `view_cnt` INT NOT NULL DEFAULT 0 COMMENT '近7天浏览数',
  `cart_cnt` INT NOT NULL DEFAULT 0 COMMENT '近7天加购数',
  `order_cnt` INT NOT NULL DEFAULT 0 COMMENT '近7天下单数',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_date` (`product_id`, `stat_date`),
  INDEX `idx_date_score` (`stat_date`, `hot_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品热度表';
