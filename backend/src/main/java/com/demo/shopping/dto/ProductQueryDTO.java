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
    /** 排序方式: price-asc, price-desc, name-asc, name-desc, newest */
    private String sort;
    private Integer page = 1;
    private Integer size = 12;
}
