package com.demo.shopping.api;

import com.demo.shopping.common.JwtUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 测试专用配置
 * 1. 提供 JwtUtils 实例，使 JwtInterceptor 在 @WebMvcTest 环境下正常工作
 * 2. 覆盖 Jackson 消息转换器，设置 UTF-8 默认字符集
 *    使 MockHttpServletResponse.getContentAsString() 正确解码中文
 * Mapper 接口的 Bean 通过 @MockBean 在 BaseApiTest 中逐个替换
 */
@TestConfiguration
public class TestJwtConfig {

    @Bean
    public JwtUtils jwtUtils() {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "secret",
                "ZGVtbw==c2hvcHBpbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmctMjAyNg==");
        ReflectionTestUtils.setField(utils, "expiration", 86400000L);
        return utils;
    }

    /**
     * 覆盖默认的 Jackson 消息转换器，设置 UTF-8 为默认字符集
     * 解决 MockHttpServletResponse.getContentAsString() 中文乱码问题
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        return converter;
    }
}
