package com.demo.shopping.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计数据返回对象
 */
@Data
public class StatsVO {

    private Long totalUsers;
    private Long totalProducts;
    private Long totalOrders;
    private Long pendingOrders;
    private BigDecimal totalRevenue;
    private Long todayOrders;
    private Long todayUsers;

    /** 各状态订单数 */
    private Map<String, Long> orderStatusCount;

    /** 近7天每日订单数 */
    private List<Map<String, Object>> dailyOrders;

    /** 各分类商品数 */
    private List<Map<String, Object>> categoryProductCount;
}
