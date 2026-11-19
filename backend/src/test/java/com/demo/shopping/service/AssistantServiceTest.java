package com.demo.shopping.service;

import com.demo.shopping.common.BusinessException;
import com.demo.shopping.dto.AssistantAnswerVO;
import com.demo.shopping.entity.ChatUnanswered;
import com.demo.shopping.mapper.ChatUnansweredMapper;
import com.demo.shopping.recommend.LlmClient;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 客服助手单元测试
 * 覆盖：知识库加载与命中、未命中转大模型、大模型异常兜底、未配置 Key 降级、
 *      每日调用限流、未命中问题落库、文本归一化与相似度计算
 */
@Epic("Shopping System")
@Feature("客服助手")
@DisplayName("客服助手测试")
@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private ChatUnansweredMapper unansweredMapper;

    @InjectMocks
    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(assistantService, "matchThreshold", 0.35);
        ReflectionTestUtils.setField(assistantService, "dailyLimit", 3);
        assistantService.init();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("知识库加载 - 24 条 FAQ 全部可用")
    void faq_loaded() {
        assertThat(assistantService.listFaqItems()).hasSize(24);
        assertThat(assistantService.listFaqItems().get(0).getQuestion()).isNotBlank();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("命中知识库 - 直接返回预置答案且不调用大模型")
    void ask_hitFaq_noLlmCall() throws Exception {
        AssistantAnswerVO vo = assistantService.ask(1L, "怎么取消订单");

        Allure.step("校验：来源为 FAQ，命中「取消订单」相关条目");
        assertThat(vo.getSource()).isEqualTo("FAQ");
        assertThat(vo.getMatchedQuestion()).contains("取消订单");
        assertThat(vo.getAnswer()).contains("待发货");

        Allure.step("校验：未调用大模型、未写入未命中记录");
        verify(llmClient, never()).chat(anyString(), anyString(), anyBoolean());
        verify(unansweredMapper, never()).insert(any(ChatUnanswered.class));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("未命中知识库 - 转大模型回答并记录问题")
    void ask_miss_usesLlm() throws Exception {
        when(llmClient.isEnabled()).thenReturn(true);
        when(llmClient.chat(anyString(), anyString(), anyBoolean())).thenReturn("这是模型生成的回答");

        AssistantAnswerVO vo = assistantService.ask(1L, "你们的藏品有鉴定证书吗");

        assertThat(vo.getSource()).isEqualTo("LLM");
        assertThat(vo.getAnswer()).isEqualTo("这是模型生成的回答");
        verify(unansweredMapper, times(1)).insert(any(ChatUnanswered.class));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("大模型调用失败 - 返回兜底话术且不影响接口")
    void ask_llmFails_fallback() throws Exception {
        when(llmClient.isEnabled()).thenReturn(true);
        when(llmClient.chat(anyString(), anyString(), anyBoolean()))
                .thenThrow(new IllegalStateException("timeout"));

        AssistantAnswerVO vo = assistantService.ask(1L, "你们的藏品有鉴定证书吗");

        assertThat(vo.getSource()).isEqualTo("FALLBACK");
        assertThat(vo.getAnswer()).contains("联系管理员");
        verify(unansweredMapper, times(1)).insert(any(ChatUnanswered.class));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("未配置 Key - 直接兜底，不扣减次数")
    void ask_llmDisabled_fallback() throws Exception {
        when(llmClient.isEnabled()).thenReturn(false);

        AssistantAnswerVO vo = assistantService.ask(1L, "随便问一个知识库没有的问题");

        assertThat(vo.getSource()).isEqualTo("FALLBACK");
        assertThat(vo.getRemaining()).isNull();
        verify(llmClient, never()).chat(anyString(), anyString(), anyBoolean());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("每日限流 - 超过次数上限后拒绝并提示")
    void ask_rateLimited() throws Exception {
        when(llmClient.isEnabled()).thenReturn(true);
        when(llmClient.chat(anyString(), anyString(), anyBoolean())).thenReturn("回答");

        assistantService.ask(2L, "问题甲甲乙乙丙丙");
        assistantService.ask(2L, "问题丁丁戊戊己己");
        assistantService.ask(2L, "问题庚庚辛辛壬壬");

        assertThatThrownBy(() -> assistantService.ask(2L, "问题癸癸子子丑丑"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("次数已用完");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("次数统计 - 命中知识库不消耗次数")
    void ask_faqHit_doesNotConsumeQuota() throws Exception {
        when(llmClient.isEnabled()).thenReturn(true);
        when(llmClient.chat(anyString(), anyString(), anyBoolean())).thenReturn("回答");

        assertThat(assistantService.remaining(3L)).isEqualTo(3);
        assistantService.ask(3L, "怎么取消订单");
        assertThat(assistantService.remaining(3L)).isEqualTo(3);

        assistantService.ask(3L, "完全无关的问题内容");
        assertThat(assistantService.remaining(3L)).isEqualTo(2);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("文本归一化 - 去除标点空白并转小写")
    void normalize_works() {
        assertThat(AssistantService.normalize("  怎么 取消 订单？ ")).isEqualTo("怎么取消订单");
        assertThat(AssistantService.normalize("AbC-123")).isEqualTo("abc123");
        assertThat(AssistantService.normalize(null)).isEmpty();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("二元组相似度 - 完全相同为 1，毫无重合为 0")
    void bigramJaccard_works() {
        assertThat(AssistantService.bigramJaccard("怎么取消订单", "怎么取消订单")).isEqualTo(1.0);
        assertThat(AssistantService.bigramJaccard("怎么取消订单", "今天天气如何")).isEqualTo(0.0);
    }
}
