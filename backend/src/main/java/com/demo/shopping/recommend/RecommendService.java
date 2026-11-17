package com.demo.shopping.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.config.RecommendProperties;
import com.demo.shopping.dto.RecommendItemVO;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.ProductHot;
import com.demo.shopping.entity.UserBehavior;
import com.demo.shopping.entity.UserProfile;
import com.demo.shopping.mapper.InteractionMapper;
import com.demo.shopping.mapper.ProductHotMapper;
import com.demo.shopping.mapper.UserBehaviorMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推荐核心服务：热度榜、相似藏品、个性化推荐。
 *
 * 三种召回策略：
 * 1) 热度召回：近 7 天多行为加权，解决冷启动与"热门藏品"需求；
 * 2) 内容过滤：用户画像标签向量与商品标签向量的余弦相似度；
 * 3) 协同过滤：物品共现矩阵的 Item-CF，作为个性化补充。
 *
 * 最终按 α·内容 + β·热度 + γ·协同 加权融合：
 * - 老用户：三种信号都参与；
 * - 新用户（无行为）：退化为纯热度，保证永远有结果可展示。
 */
@Service
public class RecommendService {

    /** 商品向量中分类伪标签的权重，分类是最强的同质信号 */
    private static final double CATEGORY_WEIGHT = 1.5;

    @Resource
    private CatalogCache catalogCache;

    @Resource
    private ProfileService profileService;

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private ProductHotMapper productHotMapper;

    @Resource
    private InteractionMapper interactionMapper;

    @Resource
    private RecommendProperties properties;

    // ==================== 对外接口 ====================

    /**
     * 热门藏品榜。
     */
    public List<RecommendItemVO> hot(Long categoryId, int limit) {
        Map<Long, Double> hotScore = hotScores();
        List<Product> candidates = availableProducts(categoryId);

        List<Map.Entry<Product, Double>> ranked = new ArrayList<>();
        for (Product product : candidates) {
            ranked.add(new HashMap.SimpleEntry<>(product, hotScore.getOrDefault(product.getId(), 0.0)));
        }
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<RecommendItemVO> result = new ArrayList<>();
        for (Map.Entry<Product, Double> entry : ranked) {
            if (result.size() >= limit) {
                break;
            }
            Product product = entry.getKey();
            ProductHot hot = latestHot(product.getId());
            result.add(toVO(product, entry.getValue(), hotReason(hot), "HOT"));
        }
        return result;
    }

    /**
     * 相似藏品（内容过滤），用于详情页"猜你喜欢"。
     */
    public List<RecommendItemVO> similar(Long productId, int limit) {
        Product origin = catalogCache.product(productId);
        if (origin == null) {
            return hot(null, limit);
        }
        Map<String, Double> originVector = productVector(origin);

        List<Map.Entry<Product, Double>> ranked = new ArrayList<>();
        for (Product product : availableProducts(null)) {
            if (product.getId().equals(productId)) {
                continue;
            }
            double sim = cosine(originVector, productVector(product));
            if (sim > 0) {
                ranked.add(new HashMap.SimpleEntry<>(product, sim));
            }
        }
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<RecommendItemVO> result = new ArrayList<>();
        for (Map.Entry<Product, Double> entry : ranked) {
            if (result.size() >= limit) {
                break;
            }
            Product product = entry.getKey();
            String shared = sharedTag(origin, product);
            String reason = shared != null ? "同属「" + shared + "」" : "同类藏品";
            result.add(toVO(product, entry.getValue(), reason, "CONTENT"));
        }
        // 内容相似度不足时用热度补齐，避免出现空推荐
        if (result.size() < limit) {
            Set<Long> picked = new HashSet<>();
            for (RecommendItemVO vo : result) {
                picked.add(vo.getId());
            }
            picked.add(productId);
            for (RecommendItemVO vo : hot(null, limit * 2)) {
                if (result.size() >= limit) {
                    break;
                }
                if (picked.add(vo.getId())) {
                    // 兼底项来自热度召回，得分打折后排在内容过滤结果之后，避免出现 1.0 分垫底
                    vo.setScore(Math.round(vo.getScore() * 0.5 * 10000.0) / 10000.0);
                    result.add(vo);
                }
            }
        }
        return result;
    }

    /**
     * 个性化推荐（混合加权），用于首页"为你推荐"。
     */
    public List<RecommendItemVO> forYou(Long userId, int limit) {
        UserProfile profile = profileService.profileOf(userId);
        Map<String, Double> userVector = buildUserVector(profile);
        boolean coldStart = userVector.isEmpty();

        Map<Long, Double> contentScore = coldStart
                ? new HashMap<>()
                : contentScores(userVector);
        Map<Long, Double> hotScore = hotScores();
        Map<Long, Double> cfScore = coldStart ? new HashMap<>() : cfScores(userId);

        Set<Long> ordered = orderedProductIds(userId);

        List<Map.Entry<Product, Double>> ranked = new ArrayList<>();
        Map<Long, String> sourceMap = new HashMap<>();
        for (Product product : availableProducts(null)) {
            if (ordered.contains(product.getId())) {
                continue;
            }
            double content = contentScore.getOrDefault(product.getId(), 0.0);
            double hot = hotScore.getOrDefault(product.getId(), 0.0);
            double cf = cfScore.getOrDefault(product.getId(), 0.0);

            double score;
            String source;
            if (coldStart) {
                score = hot;
                source = "HOT";
            } else {
                score = properties.getHybridAlpha() * content
                        + properties.getHybridBeta() * hot
                        + properties.getHybridGamma() * cf;
                source = "MIX";
                if (content >= hot && content >= cf) {
                    source = "CONTENT";
                } else if (cf > content && cf >= hot) {
                    source = "CF";
                } else if (content == 0 && cf == 0) {
                    source = "HOT";
                }
            }
            ranked.add(new HashMap.SimpleEntry<>(product, score));
            sourceMap.put(product.getId(), source);
        }
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<RecommendItemVO> result = new ArrayList<>();
        for (Map.Entry<Product, Double> entry : ranked) {
            if (result.size() >= limit) {
                break;
            }
            Product product = entry.getKey();
            String source = sourceMap.get(product.getId());
            String reason = reasonOf(userVector, product, source, coldStart);
            result.add(toVO(product, entry.getValue(), reason, source));
        }
        return result;
    }

    // ==================== 召回与打分 ====================

    /**
     * 内容过滤得分：用户画像向量与每个商品向量的余弦相似度，归一化到 [0,1]。
     */
    private Map<Long, Double> contentScores(Map<String, Double> userVector) {
        Map<Long, Double> raw = new HashMap<>();
        double max = 0;
        for (Product product : availableProducts(null)) {
            double sim = cosine(userVector, productVector(product));
            raw.put(product.getId(), sim);
            max = Math.max(max, sim);
        }
        return normalizeMap(raw, max);
    }

    /**
     * Item-CF：先由订单与购物车构建物品共现矩阵，
     * 再用用户历史交互物品的相似度加权预测。
     */
    private Map<Long, Double> cfScores(Long userId) {
        Map<Long, Map<Long, Double>> similarity = itemSimilarity();
        if (similarity.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, Double> userItems = userItemWeights(userId);
        Map<Long, Double> raw = new HashMap<>();
        double max = 0;
        for (Map.Entry<Long, Double> entry : userItems.entrySet()) {
            Long item = entry.getKey();
            double weight = entry.getValue();
            Map<Long, Double> neighbors = similarity.get(item);
            if (neighbors == null) {
                continue;
            }
            for (Map.Entry<Long, Double> nb : neighbors.entrySet()) {
                Long candidate = nb.getKey();
                if (candidate.equals(item) || userItems.containsKey(candidate)) {
                    continue;
                }
                double score = weight * nb.getValue();
                raw.merge(candidate, score, Double::sum);
                max = Math.max(max, raw.get(candidate));
            }
        }
        return normalizeMap(raw, max);
    }

    /**
     * 物品相似度矩阵：sim(i,j) = co(i,j) / sqrt(deg(i) × deg(j))。
     */
    private Map<Long, Map<Long, Double>> itemSimilarity() {
        Map<String, Integer> co = new HashMap<>();
        for (Set<Long> group : groupItems()) {
            if (group.size() < 2) {
                continue;
            }
            List<Long> items = new ArrayList<>(group);
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    long a = Math.min(items.get(i), items.get(j));
                    long b = Math.max(items.get(i), items.get(j));
                    co.merge(a + ":" + b, 1, Integer::sum);
                }
            }
        }

        Map<Long, Integer> degree = new HashMap<>();
        for (Map.Entry<String, Integer> entry : co.entrySet()) {
            String[] parts = entry.getKey().split(":");
            long a = Long.parseLong(parts[0]);
            long b = Long.parseLong(parts[1]);
            degree.merge(a, entry.getValue(), Integer::sum);
            degree.merge(b, entry.getValue(), Integer::sum);
        }

        Map<Long, Map<Long, Double>> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : co.entrySet()) {
            String[] parts = entry.getKey().split(":");
            long a = Long.parseLong(parts[0]);
            long b = Long.parseLong(parts[1]);
            int degA = degree.getOrDefault(a, 0);
            int degB = degree.getOrDefault(b, 0);
            if (degA == 0 || degB == 0) {
                continue;
            }
            double sim = entry.getValue() / Math.sqrt((double) degA * degB);
            result.computeIfAbsent(a, k -> new HashMap<>()).put(b, sim);
            result.computeIfAbsent(b, k -> new HashMap<>()).put(a, sim);
        }
        return result;
    }

    /**
     * 把订单与购物车数据按"同一订单/同一用户"聚合成商品组合。
     */
    private List<Set<Long>> groupItems() {
        Map<String, Set<Long>> groups = new LinkedHashMap<>();
        // 三类共现来源：真实订单、真实购物车、模拟订单会话
        addGroups(groups, interactionMapper.orderGroups(), "O:");
        addGroups(groups, interactionMapper.cartGroups(), "C:");
        addGroups(groups, interactionMapper.simulatedOrderGroups(), "S:");

        List<Set<Long>> result = new ArrayList<>();
        for (Set<Long> group : groups.values()) {
            if (group.size() >= 2) {
                result.add(group);
            }
        }
        return result;
    }

    private void addGroups(Map<String, Set<Long>> groups, List<Map<String, Object>> rows, String prefix) {
        for (Map<String, Object> row : safe(rows)) {
            Object gid = row.get("gid");
            Long pid = asLong(row.get("pid"));
            if (gid == null || pid == null) {
                continue;
            }
            groups.computeIfAbsent(prefix + gid, k -> new HashSet<>()).add(pid);
        }
    }

    /**
     * 用户交互过的商品及衰减权重。
     */
    private Map<Long, Double> userItemWeights(Long userId) {
        Map<Long, Double> map = new HashMap<>();
        if (userId == null) {
            return map;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(properties.getProfileWindowDays());
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .ge(UserBehavior::getCreateTime, since)
                        .isNotNull(UserBehavior::getProductId));
        LocalDateTime now = LocalDateTime.now();
        for (UserBehavior behavior : behaviors) {
            double base = behavior.getWeight() == null ? 1.0 : behavior.getWeight().doubleValue();
            double decay = decayOf(behavior.getCreateTime(), now);
            map.merge(behavior.getProductId(), base * decay, Double::sum);
        }
        return map;
    }

    /**
     * 热度得分：取最近一天的 product_hot，归一化到 [0,1]。
     */
    private Map<Long, Double> hotScores() {
        LocalDate latest = latestStatDate();
        Map<Long, Double> raw = new HashMap<>();
        if (latest != null) {
            for (ProductHot hot : productHotMapper.selectList(
                    new LambdaQueryWrapper<ProductHot>().eq(ProductHot::getStatDate, latest))) {
                raw.put(hot.getProductId(), hot.getHotScore() == null ? 0 : hot.getHotScore().doubleValue());
            }
        }
        // 无热度数据时退化为"上架时间越新越靠前"
        if (raw.isEmpty()) {
            List<Product> products = new ArrayList<>(availableProducts(null));
            products.sort(Comparator.comparing(Product::getCreateTime,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            double step = products.isEmpty() ? 1 : 1.0 / products.size();
            double score = 1.0;
            for (Product product : products) {
                raw.put(product.getId(), score);
                score -= step;
            }
        }
        return normalizeMap(raw, raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(0));
    }

    private LocalDate latestStatDate() {
        List<ProductHot> latest = productHotMapper.selectList(
                new LambdaQueryWrapper<ProductHot>()
                        .orderByDesc(ProductHot::getStatDate)
                        .last("LIMIT 1"));
        return latest.isEmpty() ? null : latest.get(0).getStatDate();
    }

    // ==================== 向量构建 ====================

    /**
     * 商品向量：标签 + 分类伪标签。
     */
    private Map<String, Double> productVector(Product product) {
        Map<String, Double> vector = new HashMap<>();
        for (String tag : catalogCache.tagsOf(product.getId())) {
            vector.merge(tag, 1.0, Double::sum);
        }
        String categoryName = catalogCache.categoryName(product.getCategoryId());
        if (categoryName != null) {
            vector.merge(categoryName, CATEGORY_WEIGHT, Double::sum);
        }
        if (product.getCategoryId() != null) {
            vector.merge("CAT:" + product.getCategoryId(), CATEGORY_WEIGHT, Double::sum);
        }
        return vector;
    }

    /**
     * 用户向量：画像中的标签向量 + 分类向量。
     */
    private Map<String, Double> buildUserVector(UserProfile profile) {
        Map<String, Double> vector = new HashMap<>();
        if (profile == null) {
            return vector;
        }
        Map<String, Double> tags = profileService.parseVector(profile.getTagVector());
        Map<String, Double> categories = profileService.parseVector(profile.getCategoryVector());
        vector.putAll(tags);
        vector.putAll(categories);
        // 价格偏好作为弱信号，不参与余弦计算，避免维度污染
        return vector;
    }

    /**
     * 余弦相似度。
     */
    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            double va = entry.getValue();
            normA += va * va;
            Double vb = b.get(entry.getKey());
            if (vb != null) {
                dot += va * vb;
            }
        }
        for (double vb : b.values()) {
            normB += vb * vb;
        }
        if (normA <= 0 || normB <= 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ==================== 推荐理由 ====================

    private String reasonOf(Map<String, Double> userVector, Product product, String source, boolean coldStart) {
        if (coldStart || "HOT".equals(source)) {
            return "近期热门藏品";
        }
        if ("CF".equals(source)) {
            return "与您浏览过的藏品常被一起关注";
        }
        String tag = topMatchingTag(userVector, product);
        if (tag != null) {
            return "根据您对「" + tag + "」的关注";
        }
        return "为您精选";
    }

    /**
     * 找出贡献最大的匹配标签，作为推荐理由。
     */
    private String topMatchingTag(Map<String, Double> userVector, Product product) {
        Map<String, Double> productVector = productVector(product);
        String best = null;
        double bestWeight = 0;
        for (String key : productVector.keySet()) {
            Double weight = userVector.get(key);
            if (weight != null && weight > bestWeight) {
                best = key;
                bestWeight = weight;
            }
        }
        if (best != null && best.startsWith("CAT:")) {
            return catalogCache.categoryName(Long.parseLong(best.substring(4)));
        }
        return best;
    }

    private String sharedTag(Product a, Product b) {
        List<String> tagsA = catalogCache.tagsOf(a.getId());
        for (String tag : catalogCache.tagsOf(b.getId())) {
            if (tagsA.contains(tag)) {
                return tag;
            }
        }
        if (a.getCategoryId() != null && a.getCategoryId().equals(b.getCategoryId())) {
            return catalogCache.categoryName(a.getCategoryId());
        }
        return null;
    }

    // ==================== 工具方法 ====================

    private List<Product> availableProducts(Long categoryId) {
        List<Product> result = new ArrayList<>();
        for (Product product : catalogCache.allProducts()) {
            if (product.getStatus() == null || product.getStatus() != 1) {
                continue;
            }
            if (categoryId != null && !categoryId.equals(product.getCategoryId())) {
                continue;
            }
            result.add(product);
        }
        return result;
    }

    private Set<Long> orderedProductIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        if (userId == null) {
            return ids;
        }
        List<UserBehavior> orders = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getBehaviorType, "ORDER"));
        for (UserBehavior behavior : orders) {
            if (behavior.getProductId() != null) {
                ids.add(behavior.getProductId());
            }
        }
        return ids;
    }

    private RecommendItemVO toVO(Product product, double score, String reason, String source) {
        RecommendItemVO vo = new RecommendItemVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setImageUrl(product.getImageUrl());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(catalogCache.categoryName(product.getCategoryId()));
        vo.setStock(product.getStock());
        vo.setScore(Math.round(score * 10000.0) / 10000.0);
        vo.setReason(reason);
        vo.setSource(source);
        return vo;
    }

    /**
     * 热度榜的推荐理由：优先展示成交，其次展示关注度，避免"热度上升"这类空话。
     */
    private String hotReason(ProductHot hot) {
        if (hot == null) {
            return "近期热门藏品";
        }
        int view = hot.getViewCnt() == null ? 0 : hot.getViewCnt();
        int cart = hot.getCartCnt() == null ? 0 : hot.getCartCnt();
        int order = hot.getOrderCnt() == null ? 0 : hot.getOrderCnt();
        if (order > 0) {
            return "近7天成交 " + order + " 件";
        }
        if (cart > 0) {
            return "近7天 " + view + " 次浏览 · " + cart + " 次加购";
        }
        if (view > 0) {
            return "近7天 " + view + " 次浏览";
        }
        return "近期热门藏品";
    }

    private ProductHot latestHot(Long productId) {
        LocalDate latest = latestStatDate();
        if (latest == null) {
            return null;
        }
        List<ProductHot> rows = productHotMapper.selectList(
                new LambdaQueryWrapper<ProductHot>()
                        .eq(ProductHot::getProductId, productId)
                        .eq(ProductHot::getStatDate, latest));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Map<Long, Double> normalizeMap(Map<Long, Double> raw, double max) {
        Map<Long, Double> result = new HashMap<>();
        if (max <= 0) {
            return result;
        }
        for (Map.Entry<Long, Double> entry : raw.entrySet()) {
            result.put(entry.getKey(), entry.getValue() / max);
        }
        return result;
    }

    private double decayOf(LocalDateTime time, LocalDateTime now) {
        if (time == null) {
            return 1.0;
        }
        double days = Duration.between(time, now).toMinutes() / (60.0 * 24.0);
        if (days < 0) {
            days = 0;
        }
        return Math.exp(-properties.getDecayLambda() * days);
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

    @SuppressWarnings("unused")
    private static double asDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
