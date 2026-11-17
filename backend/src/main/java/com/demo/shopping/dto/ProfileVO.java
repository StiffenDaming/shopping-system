package com.demo.shopping.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户画像展示对象（管理端简版）
 */
@Data
public class ProfileVO {

    private Long userId;

    private String username;

    /** 活跃度: HIGH / MID / LOW */
    private String activityLevel;

    /** 价格偏好（加权均价） */
    private BigDecimal pricePref;

    private Integer viewCnt;

    private Integer cartCnt;

    private Integer orderCnt;

    /** 兴趣标签 TOP N */
    private List<TagWeight> topTags;

    /** 偏好分类 TOP N */
    private List<TagWeight> topCategories;

    private LocalDateTime updateTime;

    /**
     * 标签及其权重
     */
    @Data
    public static class TagWeight {
        private String name;
        private Double weight;

        public TagWeight() {
        }

        public TagWeight(String name, Double weight) {
            this.name = name;
            this.weight = weight;
        }
    }
}
