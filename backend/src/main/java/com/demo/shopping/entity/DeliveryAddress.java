package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体
 */
@Data
@TableName("delivery_address")
public class DeliveryAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    /** 是否默认地址: 0-否, 1-是 */
    private Integer isDefault;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
