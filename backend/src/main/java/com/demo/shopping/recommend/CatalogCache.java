package com.demo.shopping.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.entity.Category;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.ProductTag;
import com.demo.shopping.mapper.CategoryMapper;
import com.demo.shopping.mapper.ProductMapper;
import com.demo.shopping.mapper.ProductTagMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品目录缓存。
 *
 * 推荐计算需要频繁访问"商品 → 分类""商品 → 标签"等映射，
 * 商品数量少且变动低频，整体缓存到内存并定期刷新，避免每次推荐都查库。
 */
@Component
public class CatalogCache {

    /** 缓存有效期（毫秒） */
    private static final long TTL_MILLIS = 5 * 60 * 1000L;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ProductTagMapper productTagMapper;

    private volatile Snapshot snapshot;

    public void refresh() {
        Map<Long, Product> products = new HashMap<>();
        for (Product product : productMapper.selectList(null)) {
            products.put(product.getId(), product);
        }

        Map<Long, String> categoryNames = new HashMap<>();
        for (Category category : categoryMapper.selectList(null)) {
            categoryNames.put(category.getId(), category.getName());
        }

        Map<Long, List<String>> tags = new HashMap<>();
        for (ProductTag tag : productTagMapper.selectList(
                new LambdaQueryWrapper<ProductTag>())) {
            tags.computeIfAbsent(tag.getProductId(), k -> new ArrayList<>()).add(tag.getTagName());
        }

        this.snapshot = new Snapshot(products, categoryNames, tags, System.currentTimeMillis());
    }

    private Snapshot current() {
        Snapshot s = snapshot;
        if (s == null || System.currentTimeMillis() - s.loadedAt > TTL_MILLIS) {
            synchronized (this) {
                s = snapshot;
                if (s == null || System.currentTimeMillis() - s.loadedAt > TTL_MILLIS) {
                    refresh();
                    s = snapshot;
                }
            }
        }
        return s;
    }

    public Long categoryOf(Long productId) {
        Product product = current().products.get(productId);
        return product == null ? null : product.getCategoryId();
    }

    public String categoryName(Long categoryId) {
        return categoryId == null ? null : current().categoryNames.get(categoryId);
    }

    public List<String> tagsOf(Long productId) {
        return current().tags.getOrDefault(productId, Collections.emptyList());
    }

    public BigDecimal priceOf(Long productId) {
        Product product = current().products.get(productId);
        return product == null ? null : product.getPrice();
    }

    public Product product(Long productId) {
        return current().products.get(productId);
    }

    public Collection<Product> allProducts() {
        return current().products.values();
    }

    public Map<Long, String> allCategoryNames() {
        return current().categoryNames;
    }

    /**
     * 一次性加载的只读快照
     */
    private static class Snapshot {
        private final Map<Long, Product> products;
        private final Map<Long, String> categoryNames;
        private final Map<Long, List<String>> tags;
        private final long loadedAt;

        Snapshot(Map<Long, Product> products, Map<Long, String> categoryNames,
                 Map<Long, List<String>> tags, long loadedAt) {
            this.products = products;
            this.categoryNames = categoryNames;
            this.tags = tags;
            this.loadedAt = loadedAt;
        }
    }
}
