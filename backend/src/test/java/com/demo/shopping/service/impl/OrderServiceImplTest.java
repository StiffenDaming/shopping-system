package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.dto.CheckoutDTO;
import com.demo.shopping.entity.Order;
import com.demo.shopping.entity.OrderItem;
import com.demo.shopping.entity.CartItem;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.User;
import com.demo.shopping.mapper.*;
import io.qameta.allure.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 * 覆盖：下单结算、取消订单、确认收货、发货、强制完成、红点计数
 */
@Epic("Shopping System")
@Feature("订单服务")
@DisplayName("订单服务测试")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;

    @BeforeAll
    static void initTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
    }

    // ==================== 下单结算 ====================

    @Nested
    @Story("下单结算")
    @DisplayName("下单结算")
    class CheckoutTest {

        @Test
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("正常下单 - 多件商品结算成功")
        void checkout_Success() {
            Allure.step("准备购物车数据：2件商品（商品A x2, 商品B x1）");
            CartItem cartItem1 = createCartItem(1L, USER_ID, 10L, 2, "测试商品A", new BigDecimal("99.00"));
            CartItem cartItem2 = createCartItem(2L, USER_ID, 20L, 1, "测试商品B", new BigDecimal("199.00"));
            List<CartItem> cartItems = Arrays.asList(cartItem1, cartItem2);

            Allure.step("准备商品数据：商品A库存100，商品B库存50");
            Product productA = createProduct(10L, "测试商品A", new BigDecimal("99.00"), 100, 1);
            Product productB = createProduct(20L, "测试商品B", new BigDecimal("199.00"), 50, 1);

            Allure.step("准备收货信息DTO");
            CheckoutDTO dto = new CheckoutDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区xxx路123号");

            Allure.step("Mock 购物车查询、商品查询、订单插入");
            when(cartItemMapper.selectCartWithProduct(USER_ID)).thenReturn(cartItems);
            when(productMapper.selectById(10L)).thenReturn(productA);
            when(productMapper.selectById(20L)).thenReturn(productB);
            when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                return 1;
            });

            Allure.step("执行下单操作");
            String orderNo = orderService.checkout(USER_ID, dto);

            Allure.step("验证：订单号不为空且以ORD开头");
            assertThat(orderNo).isNotNull();
            assertThat(orderNo).startsWith("ORD");

            Allure.step("验证：订单已插入1次，明细已插入2次，库存已扣减2次，购物车已清空");
            verify(orderMapper, times(1)).insert(any(Order.class));
            verify(orderItemMapper, times(2)).insert(any(OrderItem.class));
            verify(productMapper, times(2)).updateById(any(Product.class));
            verify(cartItemMapper, times(1)).deleteBatchIds(anyList());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("下单失败 - 购物车为空")
        void checkout_EmptyCart() {
            Allure.step("Mock 购物车查询返回空列表");
            CheckoutDTO dto = new CheckoutDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区");
            when(cartItemMapper.selectCartWithProduct(USER_ID)).thenReturn(Collections.emptyList());

            Allure.step("执行下单操作，预期抛出异常");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.checkout(USER_ID, dto));

            Allure.step("验证异常消息为'购物车为空，无法下单'");
            assertThat(ex.getMessage()).isEqualTo("购物车为空，无法下单");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("下单失败 - 商品已下架")
        void checkout_ProductOffline() {
            Allure.step("准备测试数据：商品状态为0（已下架）");
            CartItem cartItem = createCartItem(1L, USER_ID, 10L, 2, "测试商品A", new BigDecimal("99.00"));
            Product product = createProduct(10L, "测试商品A", new BigDecimal("99.00"), 100, 0);

            CheckoutDTO dto = new CheckoutDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区");

            when(cartItemMapper.selectCartWithProduct(USER_ID)).thenReturn(List.of(cartItem));
            when(productMapper.selectById(10L)).thenReturn(product);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.checkout(USER_ID, dto));
            assertThat(ex.getMessage()).contains("不存在或已下架");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("下单失败 - 库存不足")
        void checkout_InsufficientStock() {
            Allure.step("准备测试数据：库存3，购买5");
            CartItem cartItem = createCartItem(1L, USER_ID, 10L, 5, "测试商品A", new BigDecimal("99.00"));
            Product product = createProduct(10L, "测试商品A", new BigDecimal("99.00"), 3, 1);

            CheckoutDTO dto = new CheckoutDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区");

            when(cartItemMapper.selectCartWithProduct(USER_ID)).thenReturn(List.of(cartItem));
            when(productMapper.selectById(10L)).thenReturn(product);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.checkout(USER_ID, dto));
            assertThat(ex.getMessage()).contains("库存不足");
        }
    }

    // ==================== 取消订单 ====================

    @Nested
    @Story("取消订单")
    @DisplayName("取消订单")
    class CancelOrderTest {

        @Test
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("取消成功 - 待发货订单，库存恢复")
        void cancelOrder_Success() {
            Allure.step("准备测试数据：PENDING订单，含2件商品（商品A x2, 商品B x1）");
            Order order = createOrder(ORDER_ID, USER_ID, "PENDING");
            OrderItem item1 = createOrderItem(ORDER_ID, 10L, "商品A", 2);
            OrderItem item2 = createOrderItem(ORDER_ID, 20L, "商品B", 1);
            Product productA = createProduct(10L, "商品A", new BigDecimal("99.00"), 10, 1);
            Product productB = createProduct(20L, "商品B", new BigDecimal("199.00"), 5, 1);

            Allure.step("Mock 订单查询、明细查询、商品查询");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderItemMapper.selectList(any())).thenReturn(Arrays.asList(item1, item2));
            when(productMapper.selectById(10L)).thenReturn(productA);
            when(productMapper.selectById(20L)).thenReturn(productB);

            Allure.step("执行取消订单操作");
            orderService.cancelOrder(USER_ID, ORDER_ID);

            Allure.step("验证：商品A库存恢复为12（10+2），商品B库存恢复为6（5+1）");
            assertThat(productA.getStock()).isEqualTo(12);
            assertThat(productB.getStock()).isEqualTo(6);

            Allure.step("验证：订单状态变为CANCELLED");
            assertThat(order.getStatus()).isEqualTo("CANCELLED");
            verify(orderMapper).updateById(order);
            verify(productMapper, times(2)).updateById(any(Product.class));
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("取消失败 - 订单状态不是待发货")
        void cancelOrder_WrongStatus() {
            Allure.step("准备测试数据：订单状态为SHIPPED");
            Order order = createOrder(ORDER_ID, USER_ID, "SHIPPED");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.cancelOrder(USER_ID, ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("只能取消待发货订单");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("取消失败 - 非本人订单")
        void cancelOrder_NotOwner() {
            Allure.step("准备测试数据：订单属于用户999");
            Order order = createOrder(ORDER_ID, 999L, "PENDING");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.cancelOrder(USER_ID, ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("无权操作此订单");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("取消失败 - 订单不存在")
        void cancelOrder_NotFound() {
            Allure.step("Mock 订单查询返回null");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.cancelOrder(USER_ID, ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("订单不存在");
        }
    }

    // ==================== 确认收货 ====================

    @Nested
    @Story("确认收货")
    @DisplayName("确认收货")
    class ConfirmReceiptTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("确认收货成功 - 已发货订单")
        void confirmReceipt_Success() {
            Allure.step("准备测试数据：SHIPPED状态订单");
            Order order = createOrder(ORDER_ID, USER_ID, "SHIPPED");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            Allure.step("执行确认收货操作");
            orderService.confirmReceipt(USER_ID, ORDER_ID);

            Allure.step("验证：订单状态变为COMPLETED");
            assertThat(order.getStatus()).isEqualTo("COMPLETED");
            verify(orderMapper).updateById(order);
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("确认收货失败 - 订单状态不是已发货")
        void confirmReceipt_WrongStatus() {
            Allure.step("准备测试数据：PENDING状态订单");
            Order order = createOrder(ORDER_ID, USER_ID, "PENDING");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.confirmReceipt(USER_ID, ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("只能确认已发货的订单");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("确认收货失败 - 非本人订单")
        void confirmReceipt_NotOwner() {
            Allure.step("准备测试数据：订单属于用户999");
            Order order = createOrder(ORDER_ID, 999L, "SHIPPED");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.confirmReceipt(USER_ID, ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("无权操作此订单");
        }
    }

    // ==================== 管理员发货 ====================

    @Nested
    @Story("管理员发货")
    @DisplayName("管理员发货")
    class ShipOrderTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("发货成功 - 待发货订单")
        void shipOrder_Success() {
            Allure.step("准备测试数据：PENDING状态订单");
            Order order = createOrder(ORDER_ID, USER_ID, "PENDING");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            Allure.step("执行发货操作");
            orderService.shipOrder(ORDER_ID);

            Allure.step("验证：订单状态变为SHIPPED");
            assertThat(order.getStatus()).isEqualTo("SHIPPED");
            verify(orderMapper).updateById(order);
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("发货失败 - 订单状态不是待发货")
        void shipOrder_WrongStatus() {
            Allure.step("准备测试数据：COMPLETED状态订单");
            Order order = createOrder(ORDER_ID, USER_ID, "COMPLETED");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.shipOrder(ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("只能对待发货订单进行发货操作");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("发货失败 - 订单不存在")
        void shipOrder_NotFound() {
            Allure.step("Mock 订单查询返回null");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.shipOrder(ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("订单不存在");
        }
    }

    // ==================== 管理员强制完成 ====================

    @Nested
    @Story("管理员强制完成")
    @DisplayName("管理员强制完成")
    class AdminCompleteOrderTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("强制完成成功 - 已发货订单")
        void adminCompleteOrder_Success() {
            Allure.step("准备测试数据：SHIPPED状态订单");
            Order order = createOrder(ORDER_ID, USER_ID, "SHIPPED");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            Allure.step("执行强制完成操作");
            orderService.adminCompleteOrder(ORDER_ID);

            Allure.step("验证：订单状态变为COMPLETED");
            assertThat(order.getStatus()).isEqualTo("COMPLETED");
            verify(orderMapper).updateById(order);
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("强制完成失败 - 订单状态不是已发货")
        void adminCompleteOrder_WrongStatus() {
            Allure.step("准备测试数据：PENDING状态订单");
            Order order = createOrder(ORDER_ID, USER_ID, "PENDING");
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.adminCompleteOrder(ORDER_ID));
            assertThat(ex.getMessage()).isEqualTo("只能对已发货订单进行强制完成操作");
        }
    }

    // ==================== 红点提示 ====================

    @Nested
    @Story("红点提示")
    @DisplayName("红点提示")
    class UnreadOrderCountTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("获取红点数 - 用户曾查看过订单，统计发货后的新订单")
        void getUnreadOrderCount_WithLastViewTime() {
            Allure.step("准备测试数据：用户上次查看时间为1天前");
            User user = new User();
            user.setId(USER_ID);
            user.setLastViewOrdersTime(LocalDateTime.now().minusDays(1));

            when(userMapper.selectById(USER_ID)).thenReturn(user);
            when(orderMapper.selectCount(any())).thenReturn(3L);

            Allure.step("执行查询红点数操作");
            Integer count = orderService.getUnreadOrderCount(USER_ID);

            Allure.step("验证：返回3条未读订单");
            assertThat(count).isEqualTo(3);
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("获取红点数 - 用户从未查看过订单，统计所有已发货订单")
        void getUnreadOrderCount_NoLastViewTime() {
            Allure.step("准备测试数据：用户 lastViewOrdersTime 为 null");
            User user = new User();
            user.setId(USER_ID);
            user.setLastViewOrdersTime(null);

            when(userMapper.selectById(USER_ID)).thenReturn(user);
            when(orderMapper.selectCount(any())).thenReturn(5L);

            Integer count = orderService.getUnreadOrderCount(USER_ID);
            assertThat(count).isEqualTo(5);
        }

        @Test
        @Severity(SeverityLevel.MINOR)
        @DisplayName("获取红点数 - 用户不存在")
        void getUnreadOrderCount_UserNotFound() {
            Allure.step("Mock 用户查询返回null");
            when(userMapper.selectById(USER_ID)).thenReturn(null);
            when(orderMapper.selectCount(any())).thenReturn(0L);

            Integer count = orderService.getUnreadOrderCount(USER_ID);
            assertThat(count).isEqualTo(0);
        }
    }

    // ==================== 标记已读 ====================

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("标记订单已读 - 更新用户查看时间")
    void markOrdersViewed_Success() {
        Allure.step("Mock 用户更新返回1");
        when(userMapper.update(any(), any())).thenReturn(1);

        Allure.step("执行标记已读操作");
        orderService.markOrdersViewed(USER_ID);

        Allure.step("验证：已调用 update 方法");
        verify(userMapper, times(1)).update(isNull(), any());
    }

    // ==================== 测试数据工厂方法 ====================

    @Step("创建测试购物车项：id={id}, userId={userId}, productId={productId}, qty={quantity}")
    private CartItem createCartItem(Long id, Long userId, Long productId, Integer quantity,
                                    String productName, BigDecimal price) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setProductName(productName);
        item.setPrice(price);
        return item;
    }

    @Step("创建测试商品：id={id}, name={name}, price={price}, stock={stock}, status={status}")
    private Product createProduct(Long id, String name, BigDecimal price, Integer stock, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(status);
        product.setImageUrl("http://example.com/img.jpg");
        return product;
    }

    @Step("创建测试订单：id={id}, userId={userId}, status={status}")
    private Order createOrder(Long id, Long userId, String status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("299.00"));
        return order;
    }

    @Step("创建测试订单明细：orderId={orderId}, productId={productId}, name={productName}, qty={quantity}")
    private OrderItem createOrderItem(Long orderId, Long productId, String productName, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setProductPrice(new BigDecimal("99.00"));
        item.setQuantity(quantity);
        return item;
    }
}