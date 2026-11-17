package com.demo.shopping.recommend;

import com.demo.shopping.entity.Category;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.ProductTag;
import com.demo.shopping.mapper.CategoryMapper;
import com.demo.shopping.mapper.ProductMapper;
import com.demo.shopping.mapper.ProductTagMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商品标签抽取单元测试
 * 覆盖：未配置 Key 时的规则降级、已有标签跳过
 */
@Epic("Shopping System")
@Feature("推荐模块 - 标签抽取")
@DisplayName("商品标签抽取测试")
@ExtendWith(MockitoExtension.class)
class TagExtractionServiceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ProductTagMapper productTagMapper;
    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private TagExtractionService tagExtractionService;

    private Product product() {
        Product p = new Product();
        p.setId(1L);
        p.setName("100种天然水晶矿石");
        p.setDescription("包含紫水晶、黄水晶、萤石等，标本级，带展示盒");
        p.setPrice(new BigDecimal("20.40"));
        p.setCategoryId(1L);
        p.setStatus(1);
        return p;
    }

    private Category category() {
        Category c = new Category();
        c.setId(1L);
        c.setName("文玩雅器");
        c.setSort(0);
        return c;
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-019 未配置 API Key 时降级为规则标签")
    void extractAll_llmDisabled_fallbackToRule() {
        when(llmClient.isEnabled()).thenReturn(false);
        when(productMapper.selectList(any())).thenReturn(Collections.singletonList(product()));
        when(categoryMapper.selectList(any())).thenReturn(Collections.singletonList(category()));
        when(productTagMapper.selectCount(any())).thenReturn(0L);

        Allure.step("执行标签抽取（force=true）");
        Map<String, Object> summary = tagExtractionService.extractAll(true);

        Allure.step("校验：走规则分支，标签以 RULE 来源入库");
        assertThat(summary.get("llm")).isEqualTo(0);
        assertThat(summary.get("rule")).isEqualTo(1);
        assertThat(summary.get("llmEnabled")).isEqualTo(false);

        ArgumentCaptor<ProductTag> captor = ArgumentCaptor.forClass(ProductTag.class);
        verify(productTagMapper, atLeastOnce()).insert(captor.capture());
        assertThat(captor.getAllValues()).allMatch(tag -> "RULE".equals(tag.getSource()));
        assertThat(captor.getAllValues()).extracting(ProductTag::getTagName).contains("文玩雅器");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-020 已有标签的商品在 force=false 时被跳过")
    void extractAll_existingTags_skipped() {
        when(llmClient.isEnabled()).thenReturn(false);
        when(productMapper.selectList(any())).thenReturn(Collections.singletonList(product()));
        when(productTagMapper.selectCount(any())).thenReturn(3L);

        Map<String, Object> summary = tagExtractionService.extractAll(false);

        assertThat(summary.get("skipped")).isEqualTo(1);
        assertThat(summary.get("rule")).isEqualTo(0);
        verify(productTagMapper, never()).insert(any(ProductTag.class));
        verify(productTagMapper, never()).delete(any());
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @DisplayName("force=true 时先删除旧标签再重新抽取")
    void extractAll_force_replacesExistingTags() {
        when(llmClient.isEnabled()).thenReturn(false);
        when(productMapper.selectList(any())).thenReturn(Collections.singletonList(product()));
        when(categoryMapper.selectList(any())).thenReturn(Collections.singletonList(category()));
        when(productTagMapper.selectCount(any())).thenReturn(3L);

        tagExtractionService.extractAll(true);

        verify(productTagMapper, times(1)).delete(any());
        verify(productTagMapper, atLeastOnce()).insert(any(ProductTag.class));
    }
}
