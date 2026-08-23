package com.demo.shopping.controller;

import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.entity.CartItem;
import com.demo.shopping.service.CartService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Resource
    private CartService cartService;

    @GetMapping
    public Result<List<CartItem>> list() {
        return Result.success(cartService.getCartList(UserContext.getCurrentId()));
    }

    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.success(cartService.getCartCount(UserContext.getCurrentId()));
    }

    @PostMapping
    public Result<Void> add(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.addToCart(UserContext.getCurrentId(), productId, quantity);
        return Result.success("已加入购物车", null);
    }

    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(UserContext.getCurrentId(), id, quantity);
        return Result.success("数量已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.removeFromCart(UserContext.getCurrentId(), id);
        return Result.success("已移出购物车", null);
    }
}
