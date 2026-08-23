package com.demo.shopping.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.shopping.dto.CheckoutDTO;
import com.demo.shopping.dto.StatsVO;
import com.demo.shopping.entity.Order;

public interface OrderService extends IService<Order> {

    /**
     * 用户下单（从购物车结算）
     */
    String checkout(Long userId, CheckoutDTO dto);

    /**
     * 查询用户订单列表
     */
    IPage<Order> getUserOrders(Long userId, Integer page, Integer size, String status);

    /**
     * 查询订单详情（含明细）
     */
    Order getOrderDetail(Long userId, Long orderId);

    /**
     * 用户取消订单（带事务，恢复库存）
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 用户确认收货（SHIPPED -> COMPLETED）
     */
    void confirmReceipt(Long userId, Long orderId);

    /**
     * 获取用户未读订单数（管理员发货后用户未查看的订单数）
     */
    Integer getUnreadOrderCount(Long userId);

    /**
     * 标记用户已查看订单列表（更新 last_view_orders_time）
     */
    void markOrdersViewed(Long userId);

    // ===== 管理员功能 =====

    IPage<Order> getAdminOrders(Integer page, Integer size, String status, String keyword);

    /**
     * 管理员发货（PENDING -> SHIPPED）
     */
    void shipOrder(Long orderId);

    /**
     * 管理员强制完成订单（SHIPPED -> COMPLETED）
     */
    void adminCompleteOrder(Long orderId);

    StatsVO getStatistics();
}
