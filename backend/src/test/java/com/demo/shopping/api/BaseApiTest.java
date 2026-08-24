package com.demo.shopping.api;

import com.demo.shopping.common.JwtUtils;
import com.demo.shopping.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * API 集成测试基类
 * 提供 MockMvc 基础设施、JWT Token 生成、Allure HTTP 请求/响应附件
 *
 * @MockBean 替换所有 Mapper 接口，阻止 MapperFactoryBean 创建
 * （@MapperScan 会尝试创建 Mapper Bean，需要 SqlSessionFactory，测试环境无数据库）
 */
public abstract class BaseApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtils jwtUtils;

    // ==================== Mock 所有 Mapper（阻止 MapperFactoryBean 创建） ====================

    @MockBean
    protected UserMapper userMapper;

    @MockBean
    protected CategoryMapper categoryMapper;

    @MockBean
    protected ProductMapper productMapper;

    @MockBean
    protected CartItemMapper cartItemMapper;

    @MockBean
    protected OrderMapper orderMapper;

    @MockBean
    protected OrderItemMapper orderItemMapper;

    @MockBean
    protected DeliveryAddressMapper deliveryAddressMapper;

    // ==================== Token 生成 ====================

    /**
     * 普通用户 Token（userId=2, role=USER）
     */
    protected String userToken() {
        return "Bearer " + jwtUtils.generateToken(2L, "testuser", "USER");
    }

    /**
     * 管理员 Token（userId=1, role=ADMIN）
     */
    protected String adminToken() {
        return "Bearer " + jwtUtils.generateToken(1L, "admin", "ADMIN");
    }

    // ==================== 请求执行 + Allure 附件 ====================

    /**
     * 执行 MockMvc 请求并自动记录 HTTP 请求/响应到 Allure 报告
     *
     * @param builder MockMvc 请求构建器
     * @param token   Authorization 头（null 表示不携带）
     * @return MvcResult
     */
    protected MvcResult perform(MockHttpServletRequestBuilder builder, String token) throws Exception {
        if (token != null) {
            builder.header("Authorization", token);
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        attachHttpExchange(result);
        return result;
    }

    /**
     * 执行带 JSON Body 的请求并记录到 Allure
     */
    protected MvcResult performWithBody(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        String json = body instanceof String ? (String) body : objectMapper.writeValueAsString(body);
        builder.contentType(MediaType.APPLICATION_JSON).content(json);
        return perform(builder, token);
    }

    // ==================== Allure 附件工具 ====================

    /**
     * 将 HTTP 请求和响应信息作为附件写入 Allure 报告
     */
    private void attachHttpExchange(MvcResult result) throws Exception {
        MockHttpServletRequest request = result.getRequest();
        MockHttpServletResponse response = result.getResponse();

        // ---- 请求信息 ----
        StringBuilder reqInfo = new StringBuilder();
        reqInfo.append("Method:  ").append(request.getMethod()).append("\n");
        reqInfo.append("URL:     ").append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            reqInfo.append("?").append(queryString);
        }
        reqInfo.append("\n");

        String auth = request.getHeader("Authorization");
        if (auth != null) {
            reqInfo.append("Authorization: ").append(maskToken(auth)).append("\n");
        }
        String contentType = request.getContentType();
        if (contentType != null) {
            reqInfo.append("Content-Type:  ").append(contentType).append("\n");
        }

        Allure.addAttachment("HTTP 请求", "text/plain", reqInfo.toString());

        // 请求体
        String reqBody = request.getContentAsString();
        if (reqBody != null && !reqBody.isEmpty()) {
            Allure.addAttachment("请求体", "application/json", prettyJson(reqBody));
        }

        // ---- 响应信息 ----
        Allure.addAttachment("HTTP 状态码", String.valueOf(response.getStatus()));

        String respBody = response.getContentAsString();
        if (respBody != null && !respBody.isEmpty()) {
            Allure.addAttachment("响应体", "application/json", prettyJson(respBody));
        }
    }

    /**
     * 对 Token 做脱敏处理，只保留前20个字符
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 20) {
            return token;
        }
        return token.substring(0, 20) + "...(已脱敏)";
    }

    /**
     * 尝试格式化 JSON 字符串
     */
    protected String prettyJson(String json) {
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return json;
        }
    }
}
