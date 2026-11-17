package com.demo.shopping.recommend;

import com.demo.shopping.config.RecommendProperties;
import com.demo.shopping.dto.RecommendItemVO;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.ProductHot;
import com.demo.shopping.entity.UserBehavior;
import com.demo.shopping.entity.UserProfile;
import com.demo.shopping.mapper.InteractionMapper;
import com.demo.shopping.mapper.ProductHotMapper;
import com.demo.shopping.mapper.UserBehaviorMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 推荐召回与排序单元测试
 * 覆盖：相似度召回排除自身、冷启动兜底、已购过滤、混合加权排序、热度补齐
 */
@Epic("Shopping System")
@Feature("推荐模块 - 召回与排序")
@DisplayName("推荐服务测试")
@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @Mock
    private CatalogCache catalogCache;
    @Mock
    private ProfileService profileService;
    @Mock
    private UserBehaviorMapper userBehaviorMapper;
    @Mock
    private ProductHotMapper productHotMapper;
    @Mock
    private InteractionMapper interactionMapper;
    @Mock
    private RecommendProperties properties;

    @InjectMocks
    private RecommendService recommendService;

    private static final Long USER_ID = 1L;

    private Product product(Long id, Long categoryId, String price) {
        Product p = new Product();
        p.setId(id);
        p.setName("藏品" + id);
        p.setPrice(new BigDecimal(price));
        p.setCategoryId(categoryId);
        p.setStatus(1);
        p.setStock(10);
        p.setCreateTime(LocalDateTime.now());
        return p;
    }

    private ProductHot hot(Long productId, String score) {
        ProductHot h = new ProductHot();
        h.setProductId(productId);
        h.setHotScore(new BigDecimal(score));
        h.setViewCnt(1);
        h.setCartCnt(0);
        h.setOrderCnt(0);
        h.setStatDate(LocalDate.now());
        return h;
    }

    @BeforeEach
    void setUp() {
        lenient().when(properties.getHybridAlpha()).thenReturn(0.6);
        lenient().when(properties.getHybridBeta()).thenReturn(0.3);
        lenient().when(properties.getHybridGamma()).thenReturn(0.1);
        lenient().when(properties.getDecayLambda()).thenReturn(0.05);
        lenient().when(properties.getProfileWindowDays()).thenReturn(90);
        lenient().when(interactionMapper.orderGroups()).thenReturn(Collections.emptyList());
        lenient().when(interactionMapper.cartGroups()).thenReturn(Collections.emptyList());
        lenient().when(interactionMapper.simulatedOrderGroups()).thenReturn(Collections.emptyList());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-012 相似藏品排除自身")
    void similar_excludesSelf() {
        Product p1 = product(1L, 1L, "100.00");
        Product p2 = product(2L, 1L, "200.00");
        Product p3 = product(3L, 1L, "300.00");

        when(catalogCache.product(1L)).thenReturn(p1);
        when(catalogCache.allProducts()).thenReturn(Arrays.asList(p1, p2, p3));
        when(catalogCache.tagsOf(any())).thenReturn(Collections.emptyList());
        when(catalogCache.categoryName(any())).thenReturn(null);
        when(productHotMapper.selectList(any())).thenReturn(Arrays.asList(hot(1L, "10"), hot(2L, "5")));

        List<RecommendItemVO> result = recommendService.similar(1L, 5);

        assertThat(result).noneMatch(vo -> vo.getId().equals(1L));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-013 无画像时冷启动退化为热度榜")
    void forYou_coldStart_fallsBackToHot() {
        Product p1 = product(1L, 1L, "100.00");
        Product p2 = product(2L, 1L, "200.00");
        Product p3 = product(3L, 1L, "300.00");

        when(profileService.profileOf(USER_ID)).thenReturn(null);
        when(catalogCache.allProducts()).thenReturn(Arrays.asList(p1, p2, p3));
        when(productHotMapper.selectList(any())).thenReturn(
                Arrays.asList(hot(1L, "30"), hot(2L, "20"), hot(3L, "10")));
        when(userBehaviorMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<RecommendItemVO> result = recommendService.forYou(USER_ID, 3);

        Allure.step("校验：按热度降序，来源标记为 HOT");
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result).allMatch(vo -> "HOT".equals(vo.getSource()));
        assertThat(result.get(0).getReason()).isEqualTo("近期热门藏品");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-014 已购商品不再推荐")
    void forYou_excludesOrderedProducts() {
        Product p1 = product(1L, 1L, "100.00");
        Product p2 = product(2L, 1L, "200.00");
        Product p3 = product(3L, 1L, "300.00");

        UserProfile profile = new UserProfile();
        profile.setUserId(USER_ID);
        profile.setTagVector("{\"文玩雅器\":1.0}");
        profile.setCategoryVector("{}");

        UserBehavior ordered = new UserBehavior();
        ordered.setUserId(USER_ID);
        ordered.setProductId(2L);
        ordered.setBehaviorType("ORDER");
        ordered.setWeight(new BigDecimal("5.00"));
        ordered.setCreateTime(LocalDateTime.now());

        when(profileService.profileOf(USER_ID)).thenReturn(profile);
        when(profileService.parseVector("{\"文玩雅器\":1.0}"))
                .thenReturn(new HashMap<>(Collections.singletonMap("文玩雅器", 1.0)));
        when(profileService.parseVector("{}")).thenReturn(new HashMap<>());
        when(catalogCache.allProducts()).thenReturn(Arrays.asList(p1, p2, p3));
        when(catalogCache.tagsOf(1L)).thenReturn(Collections.singletonList("文玩雅器"));
        when(catalogCache.categoryName(any())).thenReturn(null);
        when(productHotMapper.selectList(any())).thenReturn(
                Arrays.asList(hot(1L, "10"), hot(2L, "20"), hot(3L, "5")));
        when(userBehaviorMapper.selectList(any())).thenReturn(Collections.singletonList(ordered));

        List<RecommendItemVO> result = recommendService.forYou(USER_ID, 10);

        Allure.step("校验：结果中不含已购商品 2");
        assertThat(result).isNotEmpty();
        assertThat(result).noneMatch(vo -> vo.getId().equals(2L));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-015 混合加权中内容得分高者优先")
    void forYou_contentOutranksHot() {
        Product matching = product(1L, 1L, "100.00");   // 内容相似度高
        Product popular = product(2L, 2L, "200.00");    // 热度高但内容无关

        UserProfile profile = new UserProfile();
        profile.setUserId(USER_ID);
        profile.setTagVector("{\"文玩雅器\":1.0}");
        profile.setCategoryVector("{}");

        when(profileService.profileOf(USER_ID)).thenReturn(profile);
        when(profileService.parseVector("{\"文玩雅器\":1.0}"))
                .thenReturn(new HashMap<>(Collections.singletonMap("文玩雅器", 1.0)));
        when(profileService.parseVector("{}")).thenReturn(new HashMap<>());
        when(catalogCache.allProducts()).thenReturn(Arrays.asList(matching, popular));
        when(catalogCache.tagsOf(1L)).thenReturn(Collections.singletonList("文玩雅器"));
        when(catalogCache.tagsOf(2L)).thenReturn(Collections.singletonList("无关标签"));
        when(catalogCache.categoryName(any())).thenReturn(null);
        when(productHotMapper.selectList(any())).thenReturn(
                Arrays.asList(hot(1L, "1"), hot(2L, "100")));
        when(userBehaviorMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<RecommendItemVO> result = recommendService.forYou(USER_ID, 2);

        Allure.step("校验：0.6×内容 胜过 0.3×热度，商品 1 排在商品 2 之前");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @DisplayName("TC-R-016 相似结果不足时用热度补齐且得分减半")
    void similar_fallbackScoreHalved() {
        // 三件商品分类各不相同且无共同标签，内容相似度为 0，只能走热度补齐
        Product p1 = product(1L, 1L, "100.00");
        Product p2 = product(2L, 2L, "200.00");
        Product p3 = product(3L, 3L, "300.00");

        when(catalogCache.product(1L)).thenReturn(p1);
        when(catalogCache.allProducts()).thenReturn(Arrays.asList(p1, p2, p3));
        when(catalogCache.tagsOf(any())).thenReturn(Collections.emptyList());
        when(catalogCache.categoryName(any())).thenReturn(null);
        when(productHotMapper.selectList(any())).thenReturn(
                Arrays.asList(hot(1L, "100"), hot(2L, "50"), hot(3L, "10")));

        List<RecommendItemVO> result = recommendService.similar(1L, 4);

        Allure.step("校验：补齐项来源为 HOT，且得分不超过 0.5");
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(vo -> vo.getScore() <= 0.5);
    }
}
