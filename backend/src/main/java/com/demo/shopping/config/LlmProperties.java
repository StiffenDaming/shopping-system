package com.demo.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型（DeepSeek）配置。
 * API Key 只从环境变量 LLM_API_KEY 读取，不落库、不入库代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** API Key，为空表示未启用大模型能力 */
    private String apiKey;

    private String baseUrl = "https://api.deepseek.com";

    private String model = "deepseek-flash";

    private int timeoutSeconds = 30;

    /** 是否已配置 Key，未配置时相关功能自动降级为规则策略 */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
