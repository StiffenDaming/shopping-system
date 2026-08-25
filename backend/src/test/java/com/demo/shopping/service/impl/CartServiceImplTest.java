package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.entity.CartItem;
import com.demo.shopping.entity.Product;
import com.demo.shopping.mapper.CartItemMapper;
import com.demo.shopping.mapper.ProductMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 购物车服务单元测试
 * 覆盖：加入购物车、修改数量、删除、库存校验
 */
@Epic("Shopping System")
@Feature("购物车服务")
@DisplayName("购物车服务测试")
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long CART_ITEM_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartService, "baseMapper", cartItemMapper);
    }

    // ==================== 加入购物车 ====================

    @Nested
    @Story("加入购物车")
    @DisplayName("加入购物车")
    class AddToCartTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购成功 - 新商品加入购物车")
        void addToCart_NewItem_Success() {
            Allure.step("准备测试数据：商品库存100、上架状态");
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);

            Allure.step("Mock 商品查询返回上架商品，购物车查询返回空");
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(null);

            Allure.step("执行加购操作（userId=1, productId=10, quantity=2）");
            cartService.addToCart(USER_ID, PRODUCT_ID, 2);

            Allure.step("验证：购物车新增了一条记录");
            verify(cartItemMapper, times(1)).insert(any(CartItem.class));
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购成功 - 已有商品追加数量")
        void addToCart_ExistingItem_Success() {
            Allure.step("准备测试数据：商品库存100，购物车已有该商品（数量3）");
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);
            CartItem existingItem = new CartItem();
            existingItem.setId(CART_ITEM_ID);
            existingItem.setUserId(USER_ID);
            existingItem.setProductId(PRODUCT_ID);
            existingItem.setQuantity(3);

            Allure.step("Mock 商品查询和购物车查询返回已有项");
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(existingItem);

            Allure.step("执行加购操作（追加数量2，预期总数5）");
            cartService.addToCart(USER_ID, PRODUCT_ID, 2);

            Allure.step("验证：数量更新为5（3+2），未新增记录");
            assertThat(existingItem.getQuantity()).isEqualTo(5);
            verify(cartItemMapper, times(1)).updateById(existingItem);
            verify(cartItemMapper, never()).insert(any());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("加购失败 - 商品已下架")
        void addToCart_ProductOffline() {
            Allure.step("准备测试数据：商品状态为0（已下架）");
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 0);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            Allure.step("执行加购操作，预期抛出异常");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.addToCart(USER_ID, PRODUCT_ID, 1));

            Allure.step("验证异常消息为'商品不存在或已下架'");
            assertThat(ex.getMessage()).isEqualTo("商品不存在或已下架");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购失败 - 库存不足")
        void addToCart_InsufficientStock() {
            Allure.step("准备测试数据：商品库存5，购买10");
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 5, 1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            Allure.step("执行加购操作，预期抛出库存不足异常");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.addToCart(USER_ID, PRODUCT_ID, 10));

            Allure.step("验证异常消息包含'库存不足'");
            assertThat(ex.getMessage()).contains("库存不足");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购失败 - 已有商品追加后超出库存")
        void addToCart_ExistingExceedsStock() {
            Allure.step("准备测试数据：库存5，购物车已有4，再加3（4+3=7 > 5）");
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 5, 1);
            CartItem existingItem = new CartItem();
            existingItem.setId(CART_ITEM_ID);
            existingItem.setUserId(USER_ID);
            existingItem.setProductId(PRODUCT_ID);
            existingItem.setQuantity(4);

            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(existingItem);

            Allure.step("执行加购操作，预期抛出超出库存异常");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.addToCart(USER_ID, PRODUCT_ID, 3));
            assertThat(ex.getMessage()).contains("超出库存");
        }

        @Test
        @Severity(SeverityLevel.MINOR)
        @DisplayName("加购成功 - 数量为null时默认为1")
        void addToCart_NullQuantityDefaultsTo1() {
            Allure.step("准备测试数据：商品库存充足");
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(null);

            Allure.step("执行加购操作（quantity=null）");
            cartService.addToCart(USER_ID, PRODUCT_ID, null);

            Allure.step("验证：插入的记录数量为1（默认值）");
            verify(cartItemMapper, times(1)).insert(argThat(item -> item.getQuantity() == 1));
        }
    }

    // ==================== 修改数量 ====================

    @Nested
    @Story("修改购物车数量")
    @DisplayName("修改购物车数量")
    class UpdateQuantityTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("修改数量成功")
        void updateQuantity_Success() {
            Allure.step("准备测试数据：购物车项数量2，商品库存100");
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(USER_ID);
            item.setProductId(PRODUCT_ID);
            item.setQuantity(2);
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            Allure.step("执行修改数量操作（2 → 5）");
            cartService.updateQuantity(USER_ID, CART_ITEM_ID, 5);

            Allure.step("验证：数量已更新为5");
            assertThat(item.getQuantity()).isEqualTo(5);
            verify(cartItemMapper).updateById(item);
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("修改数量失败 - 超出库存")
        void updateQuantity_OverStock() {
            Allure.step("准备测试数据：库存3，尝试修改数量为10");
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(USER_ID);
            item.setProductId(PRODUCT_ID);
            item.setQuantity(2);
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 3, 1);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            Allure.step("执行修改操作，预期抛出超出库存异常");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 10));
            assertThat(ex.getMessage()).contains("超出库存");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("修改数量失败 - 数量小于1")
        void updateQuantity_LessThanOne() {
            Allure.step("尝试修改数量为0");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 0));
            assertThat(ex.getMessage()).isEqualTo("数量必须大于0");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("修改数量失败 - 购物车项不存在")
        void updateQuantity_NotFound() {
            Allure.step("Mock 购物车查询返回null");
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 5));
            assertThat(ex.getMessage()).isEqualTo("购物车项不存在");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("修改数量失败 - 非本人购物车")
        void updateQuantity_NotOwner() {
            Allure.step("准备测试数据：购物车项属于用户999（非当前用户1）");
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(999L);
            item.setProductId(PRODUCT_ID);
            item.setQuantity(2);
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 5));
            assertThat(ex.getMessage()).isEqualTo("购物车项不存在");
        }
    }

    // ==================== 删除购物车项 ====================

    @Nested
    @Story("删除购物车项")
    @DisplayName("删除购物车项")
    class RemoveFromCartTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("删除成功")
        void removeFromCart_Success() {
            Allure.step("准备测试数据：购物车项属于当前用户");
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(USER_ID);
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);

            Allure.step("执行删除操作");
            cartService.removeFromCart(USER_ID, CART_ITEM_ID);

            Allure.step("验证：已调用 deleteById");
            verify(cartItemMapper).deleteById(CART_ITEM_ID);
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("删除失败 - 购物车项不存在")
        void removeFromCart_NotFound() {
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.removeFromCart(USER_ID, CART_ITEM_ID));
            assertThat(ex.getMessage()).isEqualTo("购物车项不存在");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("删除失败 - 非本人购物车")
        void removeFromCart_NotOwner() {
            Allure.step("准备测试数据：购物车项属于用户999");
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(999L);
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.removeFromCart(USER_ID, CART_ITEM_ID));
            assertThat(ex.getMessage()).isEqualTo("购物车项不存在");
        }
    }

    // ==================== 获取购物车数量 ====================

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("获取购物车数量")
    void getCartCount_Success() {
        Allure.step("Mock 购物车查询返回3条记录");
        when(cartItemMapper.selectCount(any())).thenReturn(3L);

        Allure.step("执行查询操作");
        Integer count = cartService.getCartCount(USER_ID);

        Allure.step("验证：返回数量为3");
        assertThat(count).isEqualTo(3);
    }

    // ==================== 测试数据工厂方法 ====================

    @Step("创建测试商品：id={id}, name={name}, price={price}, stock={stock}, status={status}")
    private Product createProduct(Long id, String name, BigDecimal price, Integer stock, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(status);
        return product;
    }
}