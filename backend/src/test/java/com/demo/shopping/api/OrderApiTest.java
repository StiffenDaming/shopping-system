package com.demo.shopping.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.shopping.controller.OrderController;
import com.demo.shopping.dto.CheckoutDTO;
import com.demo.shopping.entity.Order;
import com.demo.shopping.entity.OrderItem;
import com.demo.shopping.service.OrderService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 订单 API 集成测试
 * 覆盖：下单、查询列表、查询详情、取消、确认收货、红点、管理员发货/完成、权限控制
 */
@Epic("Shopping System")
@Feature("订单 API")
@DisplayName("订单 API 集成测试")
@WebMvcTest(controllers = OrderController.class)
@Import(TestJwtConfig.class)
class OrderApiTest extends BaseApiTest {

    @MockBean
    private OrderService orderService;

    // ==================== 下单 ====================

    @Nested
    @Story("用户下单")
    @DisplayName("下单接口 POST /api/orders/checkout")
    class CheckoutTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("下单成功 - 返回订单号")
        void checkout_Success() throws Exception {
            Allure.step("Mock checkout 返回订单号 ORD20260101001");
            when(orderService.checkout(eq(2L), any(CheckoutDTO.class))).thenReturn("ORD20260101001");

            Allure.step("发起 POST /api/orders/checkout，请求体含收货信息");
            CheckoutDTO dto = new CheckoutDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800000001");
            dto.setReceiverAddress("北京市朝阳区A路1号");
            dto.setCartItemIds(Arrays.asList(1L, 2L));

            MvcResult result = performWithBody(post("/api/orders/checkout"), userToken(), dto);

            Allure.step("验证：HTTP 200，返回订单号");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals("下单成功", body.get("message").asText());
            assertEquals("ORD20260101001", body.get("data").asText());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("下单失败 - 收货人姓名为空（参数校验）")
        void checkout_EmptyName() throws Exception {
            Allure.step("发起 POST /api/orders/checkout，receiverName 为空");
            CheckoutDTO dto = new CheckoutDTO();
            dto.setReceiverName("");
            dto.setReceiverPhone("13800000001");
            dto.setReceiverAddress("北京市朝阳区A路1号");

            MvcResult result = performWithBody(post("/api/orders/checkout"), userToken(), dto);

            Allure.step("验证：code=400，提示收货人姓名不能为空");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(400, body.get("code").asInt());
            assertEquals("收货人姓名不能为空", body.get("message").asText());
        }
    }

    // ==================== 查询订单列表 ====================

    @Nested
    @Story("查询订单列表")
    @DisplayName("查询列表 GET /api/orders")
    class ListOrdersTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("查询成功 - 返回分页订单列表")
        void list_Success() throws Exception {
            Allure.step("Mock 订单服务返回分页数据（2条订单）");
            Order order1 = createOrder(1L, "ORD001", "PENDING", new BigDecimal("12998.00"));
            Order order2 = createOrder(2L, "ORD002", "SHIPPED", new BigDecimal("5999.00"));
            Page<Order> page = new Page<>(1, 10);
            page.setRecords(Arrays.asList(order1, order2));
            page.setTotal(2);
            when(orderService.getUserOrders(eq(2L), eq(1), eq(10), isNull())).thenReturn(page);

            Allure.step("发起 GET /api/orders?page=1&size=10，携带用户 Token");
            MvcResult result = perform(
                    get("/api/orders").param("page", "1").param("size", "10"),
                    userToken());

            Allure.step("验证：HTTP 200，返回2条订单");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals(2, body.get("data").get("records").size());
            assertEquals("ORD001", body.get("data").get("records").get(0).get("orderNo").asText());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("查询成功 - 按状态筛选")
        void list_FilterByStatus() throws Exception {
            Allure.step("Mock 订单服务返回 PENDING 状态订单");
            Order order = createOrder(1L, "ORD001", "PENDING", new BigDecimal("12998.00"));
            Page<Order> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(order));
            page.setTotal(1);
            when(orderService.getUserOrders(eq(2L), eq(1), eq(10), eq("PENDING"))).thenReturn(page);

            Allure.step("发起 GET /api/orders?page=1&size=10&status=PENDING");
            MvcResult result = perform(
                    get("/api/orders").param("page", "1").param("size", "10").param("status", "PENDING"),
                    userToken());

            Allure.step("验证：HTTP 200，返回1条 PENDING 订单");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals(1, body.get("data").get("records").size());
        }
    }

    // ==================== 查询订单详情 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("查询订单详情 GET /api/orders/{id}")
    void detail_Success() throws Exception {
        Allure.step("Mock 订单服务返回订单详情（含明细）");
        Order order = createOrder(1L, "ORD001", "PENDING", new BigDecimal("12998.00"));
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(1L);
        item.setProductId(10L);
        item.setProductName("iPhone 15");
        item.setProductPrice(new BigDecimal("5999.00"));
        item.setQuantity(2);
        order.setItems(Collections.singletonList(item));
        when(orderService.getOrderDetail(2L, 1L)).thenReturn(order);

        Allure.step("发起 GET /api/orders/1，携带用户 Token");
        MvcResult result = perform(get("/api/orders/1"), userToken());

        Allure.step("验证：HTTP 200，返回订单详情含明细");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("ORD001", body.get("data").get("orderNo").asText());
        assertEquals(1, body.get("data").get("items").size());
        assertEquals("iPhone 15", body.get("data").get("items").get(0).get("productName").asText());
    }

    // ==================== 取消订单 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("取消订单 PUT /api/orders/{id}/cancel")
    void cancel_Success() throws Exception {
        Allure.step("Mock cancelOrder 正常执行");
        doNothing().when(orderService).cancelOrder(eq(2L), eq(1L));

        Allure.step("发起 PUT /api/orders/1/cancel，携带用户 Token");
        MvcResult result = perform(put("/api/orders/1/cancel"), userToken());

        Allure.step("验证：HTTP 200，提示订单已取消");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("订单已取消", body.get("message").asText());
    }

    // ==================== 确认收货 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("确认收货 PUT /api/orders/{id}/confirm")
    void confirmReceipt_Success() throws Exception {
        Allure.step("Mock confirmReceipt 正常执行");
        doNothing().when(orderService).confirmReceipt(eq(2L), eq(1L));

        Allure.step("发起 PUT /api/orders/1/confirm，携带用户 Token");
        MvcResult result = perform(put("/api/orders/1/confirm"), userToken());

        Allure.step("验证：HTTP 200，提示已确认收货");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("已确认收货", body.get("message").asText());
    }

    // ==================== 红点提示 ====================

    @Nested
    @Story("订单红点提示")
    @DisplayName("红点提示接口")
    class UnreadBadgeTest {

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("查询未读订单数 GET /api/orders/unread-count")
        void unreadCount_Success() throws Exception {
            Allure.step("Mock 返回未读数2");
            when(orderService.getUnreadOrderCount(2L)).thenReturn(2);

            Allure.step("发起 GET /api/orders/unread-count，携带用户 Token");
            MvcResult result = perform(get("/api/orders/unread-count"), userToken());

            Allure.step("验证：HTTP 200，返回未读数2");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals(2, body.get("data").asInt());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("标记已读 PUT /api/orders/mark-read")
        void markRead_Success() throws Exception {
            Allure.step("Mock markOrdersViewed 正常执行");
            doNothing().when(orderService).markOrdersViewed(2L);

            Allure.step("发起 PUT /api/orders/mark-read，携带用户 Token");
            MvcResult result = perform(put("/api/orders/mark-read"), userToken());

            Allure.step("验证：HTTP 200，操作成功");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
        }
    }

    // ==================== 管理员功能 ====================

    @Nested
    @Story("管理员订单管理")
    @DisplayName("管理员接口 /api/orders/admin/**")
    class AdminOrderTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("管理员查询订单列表 GET /api/orders/admin")
        void adminList_Success() throws Exception {
            Allure.step("Mock 管理员订单服务返回分页数据");
            Order order = createOrder(1L, "ORD001", "PENDING", new BigDecimal("5999.00"));
            Page<Order> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(order));
            page.setTotal(1);
            when(orderService.getAdminOrders(eq(1), eq(10), isNull(), isNull())).thenReturn(page);

            Allure.step("发起 GET /api/orders/admin?page=1&size=10，携带管理员 Token");
            MvcResult result = perform(
                    get("/api/orders/admin").param("page", "1").param("size", "10"),
                    adminToken());

            Allure.step("验证：HTTP 200，返回1条订单");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals(1, body.get("data").get("records").size());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("管理员发货 PUT /api/orders/admin/{id}/ship")
        void ship_Success() throws Exception {
            Allure.step("Mock shipOrder 正常执行");
            doNothing().when(orderService).shipOrder(1L);

            Allure.step("发起 PUT /api/orders/admin/1/ship，携带管理员 Token");
            MvcResult result = perform(put("/api/orders/admin/1/ship"), adminToken());

            Allure.step("验证：HTTP 200，提示订单已发货");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals("订单已发货", body.get("message").asText());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("管理员强制完成订单 PUT /api/orders/admin/{id}/complete")
        void complete_Success() throws Exception {
            Allure.step("Mock adminCompleteOrder 正常执行");
            doNothing().when(orderService).adminCompleteOrder(1L);

            Allure.step("发起 PUT /api/orders/admin/1/complete，携带管理员 Token");
            MvcResult result = perform(put("/api/orders/admin/1/complete"), adminToken());

            Allure.step("验证：HTTP 200，提示订单已完成");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals("订单已完成", body.get("message").asText());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("普通用户访问管理员接口（403 禁止访问）")
        void adminEndpoint_UserForbidden() throws Exception {
            Allure.step("发起 PUT /api/orders/admin/1/ship，携带普通用户 Token");
            MvcResult result = perform(put("/api/orders/admin/1/ship"), userToken());

            Allure.step("验证：code=403，提示无权限");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(403, body.get("code").asInt());
            assertEquals("无权限，仅管理员可操作", body.get("message").asText());
            // 管理员发货方法不应被调用
            verify(orderService, never()).shipOrder(any());
        }
    }

    // ==================== 测试数据工厂 ====================

    @Step("创建测试订单：id={id}, orderNo={orderNo}, status={status}, totalAmount={totalAmount}")
    private Order createOrder(Long id, String orderNo, String status, BigDecimal totalAmount) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(2L);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setReceiverName("张三");
        order.setReceiverPhone("13800000001");
        order.setReceiverAddress("北京市朝阳区A路1号");
        return order;
    }
}
