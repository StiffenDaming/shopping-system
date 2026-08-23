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
     * 用户取消订单
     */
    void cancelOrder(Long userId, Long orderId);

    // ===== 管理员功能 =====
    IPage<Order> getAdminOrders(Integer page, Integer size, String status, String keyword);

    void updateOrderStatus(Long orderId, String status);

    StatsVO getStatistics();
}
