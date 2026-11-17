package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商品热度实体。
 * 按天统计的交互相上加权分，用于"热门藏品榜"与冷启动兜底。
 */
@Data
@TableName("product_hot")
public class ProductHot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 热度分 = 订单数×3 + 加购数×2 + 浏览数×1（近7天窗口） */
    private BigDecimal hotScore;

    private Integer viewCnt;

    private Integer cartCnt;

    private Integer orderCnt;

    private LocalDate statDate;
}
