package com.demo.shopping.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 客服助手提问请求
 */
@Data
public class AssistantAskDTO {

    @NotBlank(message = "问题不能为空")
    @Size(max = 200, message = "问题长度不能超过 200 字")
    private String question;
}
