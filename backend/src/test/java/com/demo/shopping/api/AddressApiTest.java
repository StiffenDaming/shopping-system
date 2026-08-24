package com.demo.shopping.api;

import com.demo.shopping.controller.AddressController;
import com.demo.shopping.dto.AddressDTO;
import com.demo.shopping.entity.DeliveryAddress;
import com.demo.shopping.service.DeliveryAddressService;
import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 收货地址 API 集成测试
 * 覆盖：查询列表、查询默认地址、新增、修改、删除、设为默认、参数校验
 */
@Epic("Shopping System")
@Feature("收货地址 API")
@DisplayName("收货地址 API 集成测试")
@WebMvcTest(controllers = AddressController.class)
@Import(TestJwtConfig.class)
class AddressApiTest extends BaseApiTest {

    @MockBean
    private DeliveryAddressService deliveryAddressService;

    // ==================== 查询地址列表 ====================

    @Nested
    @Story("查询地址列表")
    @DisplayName("查询列表 GET /api/addresses")
    class ListAddressTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("查询成功 - 返回2条地址")
        void list_Success() throws Exception {
            Allure.step("Mock 地址服务返回2条记录");
            DeliveryAddress addr1 = createAddress(1L, "张三", "13800000001", "北京市朝阳区A路1号", 1);
            DeliveryAddress addr2 = createAddress(2L, "张三", "13800000002", "上海市浦东新区B路2号", 0);
            when(deliveryAddressService.listByUserId(2L)).thenReturn(Arrays.asList(addr1, addr2));

            Allure.step("发起 GET /api/addresses，携带用户 Token");
            MvcResult result = perform(get("/api/addresses"), userToken());

            Allure.step("验证：HTTP 200，返回2条地址");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals(2, body.get("data").size());
            assertEquals("张三", body.get("data").get(0).get("receiverName").asText());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("查询成功 - 无地址记录")
        void list_Empty() throws Exception {
            Allure.step("Mock 地址服务返回空列表");
            when(deliveryAddressService.listByUserId(2L)).thenReturn(Collections.emptyList());

            MvcResult result = perform(get("/api/addresses"), userToken());

            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertTrue(body.get("data").isArray());
            assertEquals(0, body.get("data").size());
        }
    }

    // ==================== 查询默认地址 ====================

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询默认地址 GET /api/addresses/default")
    void getDefault_Success() throws Exception {
        Allure.step("Mock 返回默认地址");
        DeliveryAddress addr = createAddress(1L, "张三", "13800000001", "北京市朝阳区A路1号", 1);
        when(deliveryAddressService.getDefaultAddress(2L)).thenReturn(addr);

        Allure.step("发起 GET /api/addresses/default，携带用户 Token");
        MvcResult result = perform(get("/api/addresses/default"), userToken());

        Allure.step("验证：HTTP 200，返回默认地址");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("张三", body.get("data").get("receiverName").asText());
        assertEquals(1, body.get("data").get("isDefault").asInt());
    }

    // ==================== 新增地址 ====================

    @Nested
    @Story("新增地址")
    @DisplayName("新增地址 POST /api/addresses")
    class AddAddressTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("新增成功 - 含完整信息")
        void add_Success() throws Exception {
            Allure.step("Mock addAddress 正常执行");
            doNothing().when(deliveryAddressService).addAddress(eq(2L), any(AddressDTO.class));

            Allure.step("发起 POST /api/addresses，请求体含收货人信息");
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setReceiverPhone("13900000000");
            dto.setReceiverAddress("广州市天河区C路3号");
            dto.setIsDefault(1);

            MvcResult result = performWithBody(post("/api/addresses"), userToken(), dto);

            Allure.step("验证：HTTP 200，提示地址添加成功");
            assertEquals(200, result.getResponse().getStatus());
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(200, body.get("code").asInt());
            assertEquals("地址添加成功", body.get("message").asText());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("新增失败 - 收货人姓名为空（参数校验）")
        void add_EmptyName() throws Exception {
            Allure.step("发起 POST /api/addresses，receiverName 为空");
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("");
            dto.setReceiverPhone("13900000000");
            dto.setReceiverAddress("广州市天河区C路3号");

            MvcResult result = performWithBody(post("/api/addresses"), userToken(), dto);

            Allure.step("验证：code=400，提示收货人姓名不能为空");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(400, body.get("code").asInt());
            assertEquals("收货人姓名不能为空", body.get("message").asText());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("新增失败 - 联系电话为空（参数校验）")
        void add_EmptyPhone() throws Exception {
            Allure.step("发起 POST /api/addresses，receiverPhone 为空");
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setReceiverPhone("");
            dto.setReceiverAddress("广州市天河区C路3号");

            MvcResult result = performWithBody(post("/api/addresses"), userToken(), dto);

            Allure.step("验证：code=400，提示联系电话不能为空");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals(400, body.get("code").asInt());
            assertEquals("联系电话不能为空", body.get("message").asText());
        }
    }

    // ==================== 修改地址 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("修改地址 PUT /api/addresses/{id}")
    void update_Success() throws Exception {
        Allure.step("Mock updateAddress 正常执行");
        doNothing().when(deliveryAddressService).updateAddress(eq(2L), eq(1L), any(AddressDTO.class));

        Allure.step("发起 PUT /api/addresses/1，请求体含修改后的信息");
        AddressDTO dto = new AddressDTO();
        dto.setReceiverName("李四（改）");
        dto.setReceiverPhone("13700000000");
        dto.setReceiverAddress("深圳市南山区D路4号");

        MvcResult result = performWithBody(put("/api/addresses/1"), userToken(), dto);

        Allure.step("验证：HTTP 200，提示地址修改成功");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("地址修改成功", body.get("message").asText());
    }

    // ==================== 删除地址 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("删除地址 DELETE /api/addresses/{id}")
    void delete_Success() throws Exception {
        Allure.step("Mock deleteAddress 正常执行");
        doNothing().when(deliveryAddressService).deleteAddress(eq(2L), eq(1L));

        Allure.step("发起 DELETE /api/addresses/1，携带用户 Token");
        MvcResult result = perform(delete("/api/addresses/1"), userToken());

        Allure.step("验证：HTTP 200，提示地址已删除");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("地址已删除", body.get("message").asText());
    }

    // ==================== 设为默认地址 ====================

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("设为默认地址 PUT /api/addresses/{id}/default")
    void setDefault_Success() throws Exception {
        Allure.step("Mock setDefault 正常执行");
        doNothing().when(deliveryAddressService).setDefault(eq(2L), eq(1L));

        Allure.step("发起 PUT /api/addresses/1/default，携带用户 Token");
        MvcResult result = perform(put("/api/addresses/1/default"), userToken());

        Allure.step("验证：HTTP 200，提示已设为默认地址");
        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.get("code").asInt());
        assertEquals("已设为默认地址", body.get("message").asText());
    }

    // ==================== 测试数据工厂 ====================

    private DeliveryAddress createAddress(Long id, String name, String phone,
                                            String address, Integer isDefault) {
        DeliveryAddress addr = new DeliveryAddress();
        addr.setId(id);
        addr.setUserId(2L);
        addr.setReceiverName(name);
        addr.setReceiverPhone(phone);
        addr.setReceiverAddress(address);
        addr.setIsDefault(isDefault);
        return addr;
    }
}
