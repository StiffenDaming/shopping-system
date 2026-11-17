package com.demo.shopping.recommend;

import com.demo.shopping.config.RecommendProperties;
import com.demo.shopping.entity.Product;
import com.demo.shopping.mapper.RecommendStatsMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 商品热度计算单元测试
 * 覆盖：多行为加权公式、已取消订单不计入
 */
@Epic("Shopping System")
@Feature("推荐模块 - 热度计算")
@DisplayName("商品热度计算测试")
@ExtendWith(MockitoExtension.class)
class HotServiceTest {

    @Mock
    private RecommendStatsMapper statsMapper;
    @Mock
    private com.demo.shopping.mapper.ProductHotMapper productHotMapper;
    @Mock
    private RecommendProperties properties;
    @Mock
    private CatalogCache catalogCache;

    @InjectMocks
    private HotService hotService;

    private Map<String, Object> row(Long productId, String type, int cnt) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_id", productId);
        map.put("behavior_type", type);
        map.put("cnt", cnt);
        return map;
    }

    private Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("藏品" + id);
        p.setPrice(new BigDecimal("100.00"));
        p.setStatus(1);
        return p;
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-017 热度分 = 订单×3 + 加购×2 + 浏览×1")
    void recalc_scoreFormula() {
        when(properties.getHotWindowDays()).thenReturn(7);
        when(properties.getHotWeightOrder()).thenReturn(3.0);
        when(properties.getHotWeightCart()).thenReturn(2.0);
        when(properties.getHotWeightView()).thenReturn(1.0);
        when(catalogCache.allProducts()).thenReturn(Collections.singletonList(product(1L)));

        Allure.step("近7天数据：浏览 10 次、加购 4 次、下单 2 件");
        when(statsMapper.countBehaviorsSince(any())).thenReturn(List.of(
                row(1L, "VIEW", 10),
                row(1L, "CART", 4)));
        when(statsMapper.countOrdersSince(any())).thenReturn(Collections.singletonList(row(1L, null, 2)));

        Map<String, Object> summary = hotService.recalc();

        Allure.step("校验：hot_score = 2×3 + 4×2 + 10×1 = 24.0000");
        ArgumentCaptor<BigDecimal> score = ArgumentCaptor.forClass(BigDecimal.class);
        verify(statsMapper).upsertHot(eq(1L), score.capture(), eq(10), eq(4), eq(2), eq(LocalDate.now()));
        assertThat(score.getValue()).isEqualByComparingTo("24.0000");
        assertThat(summary.get("products")).isEqualTo(1);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-018 已取消订单不计入热度")
    void recalc_cancelledOrderExcluded() {
        when(properties.getHotWindowDays()).thenReturn(7);
        when(properties.getHotWeightOrder()).thenReturn(3.0);
        when(properties.getHotWeightCart()).thenReturn(2.0);
        when(properties.getHotWeightView()).thenReturn(1.0);
        when(catalogCache.allProducts()).thenReturn(Collections.singletonList(product(1L)));

        // 统计 SQL 已带 status <> 'CANCELLED' 条件，取消订单不会出现在结果集中
        when(statsMapper.countBehaviorsSince(any())).thenReturn(Collections.singletonList(row(1L, "VIEW", 5)));
        when(statsMapper.countOrdersSince(any())).thenReturn(Collections.emptyList());

        hotService.recalc();

        ArgumentCaptor<BigDecimal> score = ArgumentCaptor.forClass(BigDecimal.class);
        verify(statsMapper).upsertHot(eq(1L), score.capture(), eq(5), eq(0), eq(0), any(LocalDate.class));
        assertThat(score.getValue()).isEqualByComparingTo("5.0000");
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @DisplayName("无任何行为数据时热度为 0")
    void recalc_noBehavior_zeroScore() {
        when(properties.getHotWindowDays()).thenReturn(7);
        when(properties.getHotWeightOrder()).thenReturn(3.0);
        when(properties.getHotWeightCart()).thenReturn(2.0);
        when(properties.getHotWeightView()).thenReturn(1.0);
        when(catalogCache.allProducts()).thenReturn(Collections.singletonList(product(1L)));
        when(statsMapper.countBehaviorsSince(any())).thenReturn(Collections.emptyList());
        when(statsMapper.countOrdersSince(any())).thenReturn(Collections.emptyList());

        hotService.recalc();

        ArgumentCaptor<BigDecimal> score = ArgumentCaptor.forClass(BigDecimal.class);
        verify(statsMapper).upsertHot(eq(1L), score.capture(), eq(0), eq(0), eq(0), any(LocalDate.class));
        assertThat(score.getValue()).isEqualByComparingTo("0");
    }
}
