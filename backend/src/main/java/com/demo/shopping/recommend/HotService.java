package com.demo.shopping.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.config.RecommendProperties;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.ProductHot;
import com.demo.shopping.mapper.ProductHotMapper;
import com.demo.shopping.mapper.RecommendStatsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 热门藏品计算服务。
 *
 * 热度分 = 订单数×3 + 加购数×2 + 浏览数×1（近 7 天窗口）。
 * 相比单纯按销量排序，多行为加权能更早捕捉到"正在升温"的藏品；
 * 由于只统计近 7 天，天然具备时间衰减效果。
 */
@Slf4j
@Service
public class HotService {

    @Resource
    private RecommendStatsMapper statsMapper;

    @Resource
    private ProductHotMapper productHotMapper;

    @Resource
    private RecommendProperties properties;

    @Resource
    private CatalogCache catalogCache;

    /**
     * 重算当日热度并写入 product_hot。
     */
    public Map<String, Object> recalc() {
        LocalDateTime since = LocalDateTime.now().minusDays(properties.getHotWindowDays());

        Map<Long, Integer> viewCounts = new HashMap<>();
        Map<Long, Integer> cartCounts = new HashMap<>();
        for (Map<String, Object> row : safe(statsMapper.countBehaviorsSince(since))) {
            Long productId = asLong(row.get("product_id"));
            if (productId == null) {
                continue;
            }
            int cnt = asInt(row.get("cnt"));
            if ("VIEW".equalsIgnoreCase(String.valueOf(row.get("behavior_type")))) {
                viewCounts.merge(productId, cnt, Integer::sum);
            } else {
                cartCounts.merge(productId, cnt, Integer::sum);
            }
        }

        Map<Long, Integer> orderCounts = new HashMap<>();
        for (Map<String, Object> row : safe(statsMapper.countOrdersSince(since))) {
            Long productId = asLong(row.get("product_id"));
            if (productId == null) {
                continue;
            }
            orderCounts.merge(productId, asInt(row.get("cnt")), Integer::sum);
        }

        LocalDate today = LocalDate.now();
        int written = 0;
        for (Product product : catalogCache.allProducts()) {
            Long id = product.getId();
            int view = viewCounts.getOrDefault(id, 0);
            int cart = cartCounts.getOrDefault(id, 0);
            int order = orderCounts.getOrDefault(id, 0);
            BigDecimal score = BigDecimal.valueOf(
                            properties.getHotWeightOrder() * order
                                    + properties.getHotWeightCart() * cart
                                    + properties.getHotWeightView() * view)
                    .setScale(4, RoundingMode.HALF_UP);
            statsMapper.upsertHot(id, score, view, cart, order, today);
            written++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("products", written);
        summary.put("statDate", today.toString());
        summary.put("windowDays", properties.getHotWindowDays());
        log.info("商品热度重算完成: {}", summary);
        return summary;
    }

    /**
     * 读取最新一天的热度榜。
     *
     * @param categoryId 可选分类过滤
     */
    public List<ProductHot> topHot(Long categoryId, int limit) {
        LocalDate latest = latestStatDate();
        LambdaQueryWrapper<ProductHot> wrapper = new LambdaQueryWrapper<>();
        if (latest != null) {
            wrapper.eq(ProductHot::getStatDate, latest);
        }
        wrapper.orderByDesc(ProductHot::getHotScore).last("LIMIT " + Math.max(limit * 5, 50));
        List<ProductHot> rows = productHotMapper.selectList(wrapper);

        List<ProductHot> result = new ArrayList<>();
        for (ProductHot hot : rows) {
            Long productId = hot.getProductId();
            Long productCategory = catalogCache.categoryOf(productId);
            if (productCategory == null) {
                continue;
            }
            Product product = catalogCache.product(productId);
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                continue;
            }
            if (categoryId != null && !categoryId.equals(productCategory)) {
                continue;
            }
            result.add(hot);
            if (result.size() >= limit) {
                break;
            }
        }

        // 热度表为空时（系统刚启动）退化为按上架时间排序，保证界面不空白
        if (result.isEmpty()) {
            List<Product> products = new ArrayList<>(catalogCache.allProducts());
            products.removeIf(p -> p.getStatus() == null || p.getStatus() != 1);
            if (categoryId != null) {
                products.removeIf(p -> !categoryId.equals(p.getCategoryId()));
            }
            products.sort(Comparator.comparing(Product::getCreateTime,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            for (Product product : products) {
                ProductHot hot = new ProductHot();
                hot.setProductId(product.getId());
                hot.setHotScore(BigDecimal.ZERO);
                hot.setViewCnt(0);
                hot.setCartCnt(0);
                hot.setOrderCnt(0);
                result.add(hot);
                if (result.size() >= limit) {
                    break;
                }
            }
        }

        // 附带商品分类名，便于前端展示
        for (ProductHot hot : result) {
            Product product = catalogCache.product(hot.getProductId());
            if (product != null) {
                hot.setStatDate(latest);
            }
        }
        return result;
    }

    private LocalDate latestStatDate() {
        List<ProductHot> latest = productHotMapper.selectList(
                new LambdaQueryWrapper<ProductHot>()
                        .orderByDesc(ProductHot::getStatDate)
                        .last("LIMIT 1"));
        return latest.isEmpty() ? null : latest.get(0).getStatDate();
    }

    private static List<Map<String, Object>> safe(List<Map<String, Object>> rows) {
        return rows == null ? new ArrayList<>() : rows;
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
