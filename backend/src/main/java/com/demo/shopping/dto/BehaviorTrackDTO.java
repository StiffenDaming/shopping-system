package com.demo.shopping.dto;

import lombok.Data;

/**
 * 行为埋点上报参数
 */
@Data
public class BehaviorTrackDTO {

    /** 商品ID（SEARCH 行为可为空） */
    private Long productId;

    /** 行为类型: VIEW / CART / ORDER / SEARCH */
    private String behaviorType;

    /** 页面停留时长（毫秒），仅 VIEW 有效 */
    private Integer stayMs;

    /** 搜索关键词，仅 SEARCH 有效 */
    private String keyword;

    /** 来源场景: LIST / DETAIL / CART / SEARCH */
    private String scene;
}
