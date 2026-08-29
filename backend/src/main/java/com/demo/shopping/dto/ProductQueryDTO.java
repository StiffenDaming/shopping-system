package com.demo.shopping.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品查询条件
 */
@Data
public class ProductQueryDTO {

    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    /** 排序方式: price-asc, price-desc, random；空值=默认（最新上架，按 createTime 倒序） */
    private String sort;
    private Integer page = 1;
    private Integer size = 12;
}
