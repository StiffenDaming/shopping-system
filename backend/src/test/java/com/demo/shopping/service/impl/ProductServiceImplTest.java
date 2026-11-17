package com.demo.shopping.service.impl;

import com.demo.shopping.common.BusinessException;
import com.demo.shopping.entity.Product;
import com.demo.shopping.mapper.ProductMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商品服务单元测试
 * 覆盖：价格与库存的正数校验（避免通过接口写入非法值）
 */
@Epic("Shopping System")
@Feature("商品管理")
@DisplayName("商品服务测试")
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "baseMapper", productMapper);
    }

    private Product product(String name, String price, Integer stock) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price == null ? null : new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("新增商品 - 合法数据保存成功")
    void addProduct_valid() {
        productService.addProduct(product("合法商品", "99.00", 10));
        verify(productMapper, times(1)).insert(any(Product.class));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("新增商品 - 价格为负数被拒绝")
    void addProduct_negativePrice_rejected() {
        assertThatThrownBy(() -> productService.addProduct(product("非法商品", "-1", 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品价格必须大于 0");
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("新增商品 - 价格为 0 被拒绝")
    void addProduct_zeroPrice_rejected() {
        assertThatThrownBy(() -> productService.addProduct(product("白送商品", "0", 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品价格必须大于 0");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("新增商品 - 库存为负数被拒绝")
    void addProduct_negativeStock_rejected() {
        assertThatThrownBy(() -> productService.addProduct(product("非法商品", "10.00", -5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品库存不能为负数");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("编辑商品 - 负价格被拒绝")
    void updateProduct_negativePrice_rejected() {
        Product update = product("改名商品", "-9.99", 5);
        update.setId(1L);

        assertThatThrownBy(() -> productService.updateProduct(update))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品价格必须大于 0");
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("编辑商品 - 负库存被拒绝")
    void updateProduct_negativeStock_rejected() {
        Product update = product("改名商品", "9.99", -3);
        update.setId(1L);

        assertThatThrownBy(() -> productService.updateProduct(update))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品库存不能为负数");
    }
}
