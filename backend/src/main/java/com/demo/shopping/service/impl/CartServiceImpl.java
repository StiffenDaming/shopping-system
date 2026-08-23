package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.entity.CartItem;
import com.demo.shopping.entity.Product;
import com.demo.shopping.mapper.CartItemMapper;
import com.demo.shopping.mapper.ProductMapper;
import com.demo.shopping.service.CartService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 购物车服务实现
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements CartService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public List<CartItem> getCartList(Long userId) {
        return baseMapper.selectCartWithProduct(userId);
    }

    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (product.getStock() < quantity) {
            throw new BusinessException("库存不足，当前库存: " + product.getStock());
        }

        // 查询是否已在购物车
        CartItem existing = baseMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));

        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            if (newQty > product.getStock()) {
                throw new BusinessException("超出库存数量");
            }
            existing.setQuantity(newQty);
            baseMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            baseMapper.insert(item);
        }
    }

    @Override
    public void updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException("数量必须大于0");
        }
        CartItem item = baseMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        Product product = productMapper.selectById(item.getProductId());
        if (product != null && quantity > product.getStock()) {
            throw new BusinessException("超出库存数量，当前库存: " + product.getStock());
        }
        item.setQuantity(quantity);
        baseMapper.updateById(item);
    }

    @Override
    public void removeFromCart(Long userId, Long cartItemId) {
        CartItem item = baseMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        baseMapper.deleteById(cartItemId);
    }

    @Override
    public void clearCart(Long userId) {
        baseMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
    }

    @Override
    public Integer getCartCount(Long userId) {
        return Math.toIntExact(baseMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)));
    }
}
