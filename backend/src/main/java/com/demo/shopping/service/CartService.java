package com.demo.shopping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.shopping.entity.CartItem;

import java.util.List;

public interface CartService extends IService<CartItem> {

    List<CartItem> getCartList(Long userId);

    void addToCart(Long userId, Long productId, Integer quantity);

    void updateQuantity(Long userId, Long cartItemId, Integer quantity);

    void removeFromCart(Long userId, Long cartItemId);

    void clearCart(Long userId);

    Integer getCartCount(Long userId);
}
