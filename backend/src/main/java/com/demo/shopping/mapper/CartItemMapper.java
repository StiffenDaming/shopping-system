package com.demo.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.shopping.entity.CartItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CartItemMapper extends BaseMapper<CartItem> {

    /**
     * 查询用户购物车（含商品信息）
     */
    @Select("SELECT c.*, p.name AS product_name, p.price AS price, " +
            "p.image_url AS product_image, p.stock AS product_stock, p.status AS product_status " +
            "FROM cart_item c " +
            "LEFT JOIN product p ON c.product_id = p.id AND p.deleted = 0 " +
            "WHERE c.user_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    List<CartItem> selectCartWithProduct(@Param("userId") Long userId);
}
