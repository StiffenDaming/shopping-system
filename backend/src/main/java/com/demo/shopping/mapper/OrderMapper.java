package com.demo.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.shopping.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询订单（含用户名）
     */
    @Select("<script>" +
            "SELECT o.*, u.username AS username FROM orders o " +
            "LEFT JOIN sys_user u ON o.user_id = u.id AND u.deleted = 0 " +
            "WHERE o.deleted = 0 " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (o.order_no LIKE CONCAT('%',#{keyword},'%') OR u.username LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "ORDER BY o.create_time DESC" +
            "</script>")
    IPage<Order> selectOrderPage(IPage<Order> page, @Param("status") String status, @Param("keyword") String keyword);
}
