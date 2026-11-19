package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客服助手「未命中知识库」的问题记录。
 *
 * 只记录 FAQ 没命中、转而调用大模型（或直接兜底）的问题，
 * 用于后续补充 FAQ；命中 FAQ 的对话不落库，避免数据膨胀与隐私风险。
 */
@Data
@TableName("chat_unanswered")
public class ChatUnanswered {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 用户原始提问 */
    private String question;

    /** 回答来源: LLM / FALLBACK */
    private String source;

    /** FAQ 最高匹配得分（未命中时通常低于阈值） */
    private BigDecimal topScore;

    private LocalDateTime createTime;
}
