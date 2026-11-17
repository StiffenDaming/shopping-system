package com.demo.shopping.recommend;

import com.demo.shopping.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * DeepSeek 大模型客户端。
 *
 * 定位说明（论文口径）：大模型只负责"把非结构化文本转成结构化特征"这一离线脏活，
 * 推荐决策由传统可解释算法完成，因此这里只提供最基础的对话补全能力。
 *
 * 使用 JDK 11 自带 HttpClient，不引入任何第三方 HTTP 依赖。
 */
@Slf4j
@Component
public class LlmClient {

    private final LlmProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;

    public LlmClient(LlmProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * 调用对话补全，返回模型输出的文本内容。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param jsonMode     是否要求模型输出严格 JSON
     */
    public String chat(String systemPrompt, String userPrompt, boolean jsonMode) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("未配置 LLM_API_KEY，大模型能力不可用");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", 0.2);
        body.put("stream", false);
        if (jsonMode) {
            ObjectNode fmt = body.putObject("response_format");
            fmt.put("type", "json_object");
        }
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userPrompt);

        String url = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IllegalStateException("大模型接口返回 " + response.statusCode() + ": "
                    + truncate(response.body(), 300));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IllegalStateException("大模型返回内容为空: " + truncate(response.body(), 300));
        }
        return content.asText();
    }

    private HttpClient client() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                }
            }
        }
        return httpClient;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
