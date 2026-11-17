package com.demo.shopping.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 交互数据查询，用于构建协同过滤的物品共现矩阵。
 */
public interface InteractionMapper {

    /**
     * 同一订单内的商品组合（购买共现）。
     */
    @Select("SELECT oi.order_id AS gid, oi.product_id AS pid "
            + "FROM order_item oi JOIN orders o ON oi.order_id = o.id "
            + "WHERE o.deleted = 0 AND o.status <> 'CANCELLED'")
    List<Map<String, Object>> orderGroups();

    /**
     * 同一用户购物车内的商品组合（加购共现）。
     */
    @Select("SELECT user_id AS gid, product_id AS pid FROM cart_item")
    List<Map<String, Object>> cartGroups();

    /**
     * 模拟订单会话内的商品组合。
     * 模拟数据集只注入行为日志、不写入 orders 表，因此模拟下单按 scene 归组。
     */
    @Select("SELECT scene AS gid, product_id AS pid FROM user_behavior "
            + "WHERE behavior_type = 'ORDER' AND scene IS NOT NULL AND product_id IS NOT NULL")
    List<Map<String, Object>> simulatedOrderGroups();
}
