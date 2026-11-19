package com.demo.shopping.dto;

import lombok.Data;

/**
 * 客服助手推荐问题（前端展示为可点击的气泡）
 */
@Data
public class FaqItemVO {

    private String id;

    private String category;

    private String question;
}
