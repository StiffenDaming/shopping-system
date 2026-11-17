package com.demo.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.shopping.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 条件扣减库存：只有当前库存足够时才扣减，返回受影响行数。
     * 用单条原子 UPDATE 替代"读-改-写"，避免并发下单时超卖。
     */
    @Update("UPDATE product SET stock = stock - #{quantity} "
            + "WHERE id = #{id} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 条件恢复库存：取消订单时使用，单条原子 UPDATE 避免并发下的丢失更新。
     */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{id} AND deleted = 0")
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
