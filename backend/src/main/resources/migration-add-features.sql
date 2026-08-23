-- =============================================
-- 数据库迁移脚本：新增收货地址管理 + 订单红点提示功能
-- 适用于已有数据的 MySQL 实例（无需清空 Docker volume）
-- 执行方式: docker exec -i shopping-mysql mysql -uroot -p123456 shopping < migration-add-features.sql
-- =============================================

-- 1. sys_user 表新增 last_view_orders_time 字段
ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `last_view_orders_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上次查看订单列表的时间（用于红点提示）';

-- 2. 新建 delivery_address 收货地址表
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
