package com.demo.shopping.dto;

import lombok.Data;

import java.util.List;

/**
 * 客服助手回答
 */
@Data
public class AssistantAnswerVO {

    /** 回答正文 */
    private String answer;

    /** 来源: FAQ（命中知识库）/ LLM（调用大模型）/ FALLBACK（兜底话术） */
    private String source;

    /** 命中的知识库问题（未命中时为空） */
    private String matchedQuestion;

    /** 推荐的追问（点击即可继续提问） */
    private List<String> suggestions;

    /** 今日剩余的智能问答次数 */
    private Integer remaining;
}
