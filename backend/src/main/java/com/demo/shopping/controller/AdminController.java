package com.demo.shopping.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.StatsVO;
import com.demo.shopping.entity.User;
import com.demo.shopping.service.OrderService;
import com.demo.shopping.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员控制器：用户管理 + 统计数据
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private OrderService orderService;

    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        checkAdmin();
        return Result.success(orderService.getStatistics());
    }

    @GetMapping("/users")
    public Result<IPage<User>> userList(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword) {
        checkAdmin();
        return Result.success(userService.getUserPage(page, size, keyword));
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        checkAdmin();
        userService.updateUserRole(id, role);
        return Result.success("角色已更新", null);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        checkAdmin();
        userService.updateUserStatus(id, status);
        return Result.success("状态已更新", null);
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        checkAdmin();
        userService.resetPassword(id);
        return Result.success("密码已重置为123456", null);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
