package com.demo.shopping.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.CheckoutDTO;
import com.demo.shopping.entity.Order;
import com.demo.shopping.service.OrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 订单控制器：用户下单 + 管理员管理
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @PostMapping("/checkout")
    public Result<String> checkout(@Valid @RequestBody CheckoutDTO dto) {
        String orderNo = orderService.checkout(UserContext.getCurrentId(), dto);
        return Result.success("下单成功", orderNo);
    }

    @GetMapping
    public Result<IPage<Order>> myList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String status) {
        return Result.success(orderService.getUserOrders(UserContext.getCurrentId(), page, size, status));
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(UserContext.getCurrentId(), id));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(UserContext.getCurrentId(), id);
        return Result.success("订单已取消", null);
    }

    /**
     * 用户确认收货（SHIPPED -> COMPLETED）
     */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        orderService.confirmReceipt(UserContext.getCurrentId(), id);
        return Result.success("已确认收货", null);
    }

    /**
     * 获取未读订单数（用于红点提示）
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        return Result.success(orderService.getUnreadOrderCount(UserContext.getCurrentId()));
    }

    /**
     * 标记订单列表已查看（消除红点）
     */
    @PutMapping("/mark-read")
    public Result<Void> markRead() {
        orderService.markOrdersViewed(UserContext.getCurrentId());
        return Result.success();
    }

    // ===== 管理员功能 =====

    @GetMapping("/admin")
    public Result<IPage<Order>> adminList(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword) {
        checkAdmin();
        return Result.success(orderService.getAdminOrders(page, size, status, keyword));
    }

    @GetMapping("/admin/{id}")
    public Result<Order> adminDetail(@PathVariable Long id) {
        checkAdmin();
        return Result.success(orderService.getOrderDetail(null, id));
    }

    /**
     * 管理员发货（PENDING -> SHIPPED）
     */
    @PutMapping("/admin/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        checkAdmin();
        orderService.shipOrder(id);
        return Result.success("订单已发货", null);
    }

    /**
     * 管理员强制完成订单（SHIPPED -> COMPLETED）
     */
    @PutMapping("/admin/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        checkAdmin();
        orderService.adminCompleteOrder(id);
        return Result.success("订单已完成", null);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
