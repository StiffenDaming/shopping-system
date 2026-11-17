package com.demo.shopping.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 推荐模块统计查询。
 * 使用原生 SQL 完成聚合，避免把大量明细数据拉到应用层。
 */
public interface RecommendStatsMapper {

    /**
     * 统计指定时间之后各商品的下单件数（排除已取消订单）。
     */
    @Select("SELECT oi.product_id AS product_id, SUM(oi.quantity) AS cnt "
            + "FROM order_item oi JOIN orders o ON oi.order_id = o.id "
            + "WHERE o.create_time >= #{since} AND o.deleted = 0 AND o.status <> 'CANCELLED' "
            + "GROUP BY oi.product_id")
    List<Map<String, Object>> countOrdersSince(@Param("since") LocalDateTime since);

    /**
     * 统计指定时间之后各商品的浏览/加购次数。
     */
    @Select("SELECT product_id AS product_id, behavior_type AS behavior_type, COUNT(*) AS cnt "
            + "FROM user_behavior "
            + "WHERE create_time >= #{since} AND behavior_type IN ('VIEW', 'CART') AND product_id IS NOT NULL "
            + "GROUP BY product_id, behavior_type")
    List<Map<String, Object>> countBehaviorsSince(@Param("since") LocalDateTime since);

    /**
     * 写入或更新当日商品热度。
     */
    @Insert("INSERT INTO product_hot(product_id, hot_score, view_cnt, cart_cnt, order_cnt, stat_date) "
            + "VALUES(#{productId}, #{hotScore}, #{viewCnt}, #{cartCnt}, #{orderCnt}, #{statDate}) "
            + "ON DUPLICATE KEY UPDATE hot_score = VALUES(hot_score), view_cnt = VALUES(view_cnt), "
            + "cart_cnt = VALUES(cart_cnt), order_cnt = VALUES(order_cnt)")
    int upsertHot(@Param("productId") Long productId,
                  @Param("hotScore") BigDecimal hotScore,
                  @Param("viewCnt") Integer viewCnt,
                  @Param("cartCnt") Integer cartCnt,
                  @Param("orderCnt") Integer orderCnt,
                  @Param("statDate") LocalDate statDate);
}
