package com.demo.shopping.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐结果项：商品信息 + 推荐得分 + 推荐理由
 */
@Data
public class RecommendItemVO {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String imageUrl;

    private Long categoryId;

    private String categoryName;

    /** 推荐得分（0~1 归一化） */
    private Double score;

    /** 推荐理由，用于"猜你喜欢"的可解释展示 */
    private String reason;

    /** 命中来源: HOT-热度, CONTENT-内容过滤, CF-协同过滤, MIX-混合 */
    private String source;

    private Integer stock;
}
