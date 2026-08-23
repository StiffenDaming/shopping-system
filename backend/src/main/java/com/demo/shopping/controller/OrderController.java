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

    @PutMapping("/admin/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        checkAdmin();
        orderService.updateOrderStatus(id, status);
        return Result.success("订单状态已更新", null);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
