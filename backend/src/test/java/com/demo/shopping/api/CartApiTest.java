package com.demo.shopping.api;

import com.demo.shopping.controller.CartController;
import com.demo.shopping.entity.CartItem;
import com.demo.shopping.service.CartService;
import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 购物车 API 集成测试
 * 覆盖：查询列表、查询数量、加入购物车、修改数量、删除、未授权访问
 */
@Epic("Shopping System")
@Feature("购物车 API")
@DisplayName("购物车 API 集成测试")
@WebMvcTest(controllers = CartController.class)
@Import(TestJwtConfig.class)
class CartApiTest extends BaseApiTest {

    @MockBean
    private CartService cartService;

    // ==================== 查询购物车列表 ====================

    @Nested
    @Story("查询购物车列表")
    @DisplayName("查询列表 GET /api/cart")
    class ListCartTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("查询成功 - 返回2条购物车记录")
        void list_Success() throws Exception {
            Allure.step("Mock 购物车服务返回2条记录");
            CartItem item1 = createCartItem(1L, 10L, 2, "iPhone 15", new BigDecimal("5999.00"));
            CartItem item2 = createCartItem(2L, 20L, 1, "iPad Pro", new BigDecimal("7999.00"));
            when(cartService.getCartList(2L)).thenReturn(Arrays.asList(item1, item2));

            Allure.step("发起 GET /api/cart，携带用户 Token");
            MvcResult result = perform(get("/api/cart"), userToken());

            Allure.step("验证：HTTP 200，返回2条购物车项");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            assertThat(body.get("data").size()).isEqualTo(2);
            assertThat(body.get("data").get(0).get("productName").asText()).isEqualTo("iPhone 15");

            // 如果你想知道 cartService.getCartList(2L) 确实被调用了，可以加一行验证：
            // 但这属于"二次确认"，不是必须的
            verify(cartService, times(1)).getCartList(2L);
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("查询成功 - 购物车为空")
        void list_Empty() throws Exception {
            Allure.step("Mock 购物车服务返回空列表");
            when(cartService.getCartList(2L)).thenReturn(Collections.emptyList());

            Allure.step("发起 GET /api/cart，携带用户 Token");
            MvcResult result = perform(get("/api/cart"), userToken());

            Allure.step("验证：HTTP 200，返回空数组");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            assertThat(body.get("data").isArray()).isTrue();
            assertThat(body.get("data").size()).isEqualTo(0);
        }
    }

    // ==================== 查询购物车数量 ====================

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询购物车数量 GET /api/cart/count")
    void count_Success() throws Exception {
        Allure.step("Mock 购物车数量返回3");
        when(cartService.getCartCount(2L)).thenReturn(3);

        Allure.step("发起 GET /api/cart/count，携带用户 Token");
        MvcResult result = perform(get("/api/cart/count"), userToken());

        Allure.step("验证：HTTP 200，返回数量3");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(200);
        assertThat(body.get("data").asInt()).isEqualTo(3);
    }

    // ==================== 加入购物车 ====================

    @Nested
    @Story("加入购物车")
    @DisplayName("加入购物车 POST /api/cart")
    class AddToCartTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购成功 - 指定商品和数量")
        void add_Success() throws Exception {
            Allure.step("Mock addToCart 正常执行");
            doNothing().when(cartService).addToCart(eq(2L), eq(10L), eq(2));

            Allure.step("发起 POST /api/cart?productId=10&quantity=2，携带用户 Token");
            MvcResult result = perform(
                    post("/api/cart").param("productId", "10").param("quantity", "2"),
                    userToken());

            Allure.step("验证：HTTP 200，提示已加入购物车");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            assertThat(body.get("message").asText()).isEqualTo("已加入购物车");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("加购成功 - 数量默认为1")
        void add_DefaultQuantity() throws Exception {
            Allure.step("发起 POST /api/cart?productId=10（不传 quantity），携带用户 Token");
            MvcResult result = perform(
                    post("/api/cart").param("productId", "10"),
                    userToken());

            Allure.step("验证：HTTP 200，加购成功");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            verify(cartService).addToCart(eq(2L), eq(10L), eq(1));
        }
    }

    // ==================== 修改数量 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("修改数量 PUT /api/cart/{id}?quantity=5")
    void updateQuantity_Success() throws Exception {
        Allure.step("Mock updateQuantity 正常执行");
        doNothing().when(cartService).updateQuantity(eq(2L), eq(1L), eq(5));

        Allure.step("发起 PUT /api/cart/1?quantity=5，携带用户 Token");
        MvcResult result = perform(
                put("/api/cart/1").param("quantity", "5"),
                userToken());

        Allure.step("验证：HTTP 200，提示数量已更新");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(200);
        assertThat(body.get("message").asText()).isEqualTo("数量已更新");
    }

    // ==================== 删除购物车项 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("删除购物车项 DELETE /api/cart/{id}")
    void remove_Success() throws Exception {
        Allure.step("Mock removeFromCart 正常执行");
        doNothing().when(cartService).removeFromCart(eq(2L), eq(1L));

        Allure.step("发起 DELETE /api/cart/1，携带用户 Token");
        MvcResult result = perform(delete("/api/cart/1"), userToken());

        Allure.step("验证：HTTP 200，提示已移出购物车");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(200);
        assertThat(body.get("message").asText()).isEqualTo("已移出购物车");
    }

    // ==================== 未授权访问 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("未携带 Token 访问购物车（401）")
    void accessWithoutToken_Unauthorized() throws Exception {
        Allure.step("发起 GET /api/cart，不携带 Authorization 头");

        MvcResult result = perform(get("/api/cart"), null);

        Allure.step("验证：HTTP 401，提示未登录");
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(401);
        verify(cartService, never()).getCartList(any());
    }

    // ==================== 测试数据工厂 ====================

    private CartItem createCartItem(Long id, Long productId, Integer quantity,
                                    String productName, BigDecimal price) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(2L);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setProductName(productName);
        item.setPrice(price);
        return item;
    }
}