package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String email;

    private String phone;

    private String avatar;

    /** 角色: USER-普通用户, ADMIN-管理员 */
    private String role;

    /** 状态: 1-正常, 0-禁用 */
    private Integer status;

    /** 上次查看订单列表的时间（用于红点提示） */
    private LocalDateTime lastViewOrdersTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
