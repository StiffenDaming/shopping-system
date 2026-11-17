package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户行为日志实体。
 * 推荐算法的数据地基：浏览、加购、下单、搜索四类行为统一入库。
 */
@Data
@TableName("user_behavior")
public class UserBehavior {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，0 表示游客（未登录） */
    private Long userId;

    private Long productId;

    /** 行为类型: VIEW / CART / ORDER / SEARCH */
    private String behaviorType;

    /** 行为基础权重: VIEW=1, CART=3, ORDER=5, SEARCH=1 */
    private BigDecimal weight;

    /** 页面停留时长（毫秒），仅 VIEW 有效 */
    private Integer stayMs;

    /** 搜索关键词，仅 SEARCH 有效 */
    private String keyword;

    /** 来源场景: LIST / DETAIL / CART / SEARCH */
    private String scene;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
