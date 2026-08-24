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

import static org.junit.jupiter.api.Assertions.*;
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
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(null); // 购物车中不存在

            cartService.addToCart(USER_ID, PRODUCT_ID, 2);

            verify(cartItemMapper, times(1)).insert(any(CartItem.class));
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购成功 - 已有商品追加数量")
        void addToCart_ExistingItem_Success() {
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);
            CartItem existingItem = new CartItem();
            existingItem.setId(CART_ITEM_ID);
            existingItem.setUserId(USER_ID);
            existingItem.setProductId(PRODUCT_ID);
            existingItem.setQuantity(3);

            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(existingItem);

            cartService.addToCart(USER_ID, PRODUCT_ID, 2);

            assertEquals(5, existingItem.getQuantity()); // 3 + 2 = 5
            verify(cartItemMapper, times(1)).updateById(existingItem);
            verify(cartItemMapper, never()).insert(any());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("加购失败 - 商品已下架")
        void addToCart_ProductOffline() {
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 0);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.addToCart(USER_ID, PRODUCT_ID, 1));
            assertEquals("商品不存在或已下架", ex.getMessage());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购失败 - 库存不足")
        void addToCart_InsufficientStock() {
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 5, 1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.addToCart(USER_ID, PRODUCT_ID, 10));
            assertTrue(ex.getMessage().contains("库存不足"));
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("加购失败 - 已有商品追加后超出库存")
        void addToCart_ExistingExceedsStock() {
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 5, 1);
            CartItem existingItem = new CartItem();
            existingItem.setId(CART_ITEM_ID);
            existingItem.setUserId(USER_ID);
            existingItem.setProductId(PRODUCT_ID);
            existingItem.setQuantity(4);

            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(existingItem);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.addToCart(USER_ID, PRODUCT_ID, 3)); // 4+3=7 > 5
            assertTrue(ex.getMessage().contains("超出库存"));
        }

        @Test
        @Severity(SeverityLevel.MINOR)
        @DisplayName("加购成功 - 数量为null时默认为1")
        void addToCart_NullQuantityDefaultsTo1() {
            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(cartItemMapper.selectOne(any())).thenReturn(null);

            cartService.addToCart(USER_ID, PRODUCT_ID, null);

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
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(USER_ID);
            item.setProductId(PRODUCT_ID);
            item.setQuantity(2);

            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 100, 1);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            cartService.updateQuantity(USER_ID, CART_ITEM_ID, 5);

            assertEquals(5, item.getQuantity());
            verify(cartItemMapper).updateById(item);
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("修改数量失败 - 超出库存")
        void updateQuantity_OverStock() {
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(USER_ID);
            item.setProductId(PRODUCT_ID);
            item.setQuantity(2);

            Product product = createProduct(PRODUCT_ID, "测试商品", new BigDecimal("99.00"), 3, 1);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 10));
            assertTrue(ex.getMessage().contains("超出库存"));
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("修改数量失败 - 数量小于1")
        void updateQuantity_LessThanOne() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 0));
            assertEquals("数量必须大于0", ex.getMessage());
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("修改数量失败 - 购物车项不存在")
        void updateQuantity_NotFound() {
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 5));
            assertEquals("购物车项不存在", ex.getMessage());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("修改数量失败 - 非本人购物车")
        void updateQuantity_NotOwner() {
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(999L); // 其他用户
            item.setProductId(PRODUCT_ID);
            item.setQuantity(2);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.updateQuantity(USER_ID, CART_ITEM_ID, 5));
            assertEquals("购物车项不存在", ex.getMessage());
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
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(USER_ID);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);

            cartService.removeFromCart(USER_ID, CART_ITEM_ID);

            verify(cartItemMapper).deleteById(CART_ITEM_ID);
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("删除失败 - 购物车项不存在")
        void removeFromCart_NotFound() {
            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.removeFromCart(USER_ID, CART_ITEM_ID));
            assertEquals("购物车项不存在", ex.getMessage());
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("删除失败 - 非本人购物车")
        void removeFromCart_NotOwner() {
            CartItem item = new CartItem();
            item.setId(CART_ITEM_ID);
            item.setUserId(999L);

            when(cartItemMapper.selectById(CART_ITEM_ID)).thenReturn(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.removeFromCart(USER_ID, CART_ITEM_ID));
            assertEquals("购物车项不存在", ex.getMessage());
        }
    }

    // ==================== 获取购物车数量 ====================

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("获取购物车数量")
    void getCartCount_Success() {
        when(cartItemMapper.selectCount(any())).thenReturn(3L);

        Integer count = cartService.getCartCount(USER_ID);

        assertEquals(3, count);
    }

    // ==================== 测试数据工厂方法 ====================

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