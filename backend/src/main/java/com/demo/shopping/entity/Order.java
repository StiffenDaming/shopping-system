package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体（对应 orders 表）
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    /** 状态: PENDING-待发货, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消 */
    private String status;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 订单明细列表（非数据库字段） */
    @TableField(exist = false)
    private List<OrderItem> items;

    /** 用户名（非数据库字段，管理端用） */
    @TableField(exist = false)
    private String username;
}
