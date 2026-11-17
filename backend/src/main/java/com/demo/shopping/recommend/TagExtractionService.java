package com.demo.shopping.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.entity.Category;
import com.demo.shopping.entity.Product;
import com.demo.shopping.entity.ProductTag;
import com.demo.shopping.mapper.CategoryMapper;
import com.demo.shopping.mapper.ProductMapper;
import com.demo.shopping.mapper.ProductTagMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品标签抽取服务（大模型落点 L1）。
 *
 * 为什么需要它：product 表只有 name 与 description 两个自然语言字段，
 * 而基于内容的过滤必须有结构化特征向量。用大模型把商品描述一次性抽成
 * 标签入库，之后所有在线推荐都只读库、不再调用大模型——既控制了成本，
 * 也把模型的不确定性锁定在离线阶段。
 *
 * 未配置 API Key 时自动降级为词典规则，保证系统仍可运行。
 */
@Slf4j
@Service
public class TagExtractionService {

    private static final Set<String> VALID_TYPES =
            new LinkedHashSet<>(Arrays.asList("SUBJECT", "MATERIAL", "STYLE", "ERA", "SCENE"));

    private static final String SYSTEM_PROMPT =
            "你是收藏品电商平台的商品特征抽取器。你只输出 JSON，不输出任何解释文字。";

    private static final String USER_PROMPT_TEMPLATE =
            "请从下面的收藏品信息中抽取 3 到 6 个结构化标签。\n"
            + "要求：\n"
            + "1. 每个标签 2 到 6 个汉字，必须具体，不要使用“商品”“收藏品”这类空泛词；\n"
            + "2. 标签类型只能是 SUBJECT（题材品类）、MATERIAL（材质）、STYLE（风格）、ERA（年代）、SCENE（使用场景）之一；\n"
            + "3. 只输出如下 JSON 结构：{\"tags\":[{\"name\":\"紫水晶\",\"type\":\"MATERIAL\"}]}\n\n"
            + "收藏品信息：\n名称：%s\n分类：%s\n描述：%s";

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ProductTagMapper productTagMapper;

    @Resource
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 批量为商品抽取标签。
     *
     * @param force true=无论是否已有标签都重新抽取；false=跳过已有标签的商品
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> extractAll(boolean force) {
        List<Product> products = productMapper.selectList(null);
        Map<Long, String> categoryNames = loadCategoryNames();

        int llmOk = 0;
        int ruleOk = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();

        for (Product product : products) {
            long existing = productTagMapper.selectCount(
                    new LambdaQueryWrapper<ProductTag>().eq(ProductTag::getProductId, product.getId()));
            if (existing > 0 && !force) {
                skipped++;
                continue;
            }
            if (existing > 0) {
                productTagMapper.delete(
                        new LambdaQueryWrapper<ProductTag>().eq(ProductTag::getProductId, product.getId()));
            }

            String categoryName = categoryNames.getOrDefault(product.getCategoryId(), "未分类");
            List<TagPair> tags = null;
            String source = "LLM";

            if (llmClient.isEnabled()) {
                try {
                    tags = extractByLlm(product, categoryName);
                    if (tags.isEmpty()) {
                        tags = null;
                    }
                } catch (Exception e) {
                    log.warn("商品[{}]大模型标签抽取失败，降级为规则: {}", product.getName(), e.getMessage());
                }
            }
            if (tags == null) {
                tags = extractByRule(product, categoryName);
                source = "RULE";
            }

            for (TagPair tag : tags) {
                ProductTag entity = new ProductTag();
                entity.setProductId(product.getId());
                entity.setTagName(tag.name);
                entity.setTagType(tag.type);
                entity.setWeight(BigDecimal.ONE);
                entity.setSource(source);
                try {
                    productTagMapper.insert(entity);
                } catch (Exception ignore) {
                    // 唯一键冲突（同一商品重复标签）直接跳过
                }
            }
            if ("LLM".equals(source)) {
                llmOk++;
            } else {
                ruleOk++;
            }
            sleepQuietly(200L);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", products.size());
        summary.put("llm", llmOk);
        summary.put("rule", ruleOk);
        summary.put("skipped", skipped);
        summary.put("failures", failures);
        summary.put("llmEnabled", llmClient.isEnabled());
        log.info("标签抽取完成: {}", summary);
        return summary;
    }

    /**
     * 调用大模型抽取标签。
     */
    private List<TagPair> extractByLlm(Product product, String categoryName) throws Exception {
        String userPrompt = String.format(USER_PROMPT_TEMPLATE,
                safe(product.getName()),
                safe(categoryName),
                safe(product.getDescription()));

        String raw = llmClient.chat(SYSTEM_PROMPT, userPrompt, true);
        String json = normalizeJson(raw);
        JsonNode root = objectMapper.readTree(json);
        JsonNode tagsNode = root.path("tags");
        List<TagPair> result = new ArrayList<>();
        if (tagsNode.isArray()) {
            for (JsonNode node : tagsNode) {
                String name = node.path("name").asText("").trim();
                String type = node.path("type").asText("").trim().toUpperCase();
                if (name.isEmpty() || name.length() > 50) {
                    continue;
                }
                result.add(new TagPair(name, VALID_TYPES.contains(type) ? type : "SUBJECT"));
                if (result.size() >= 8) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 规则降级：分类名 + 关键词词典匹配。
     */
    private List<TagPair> extractByRule(Product product, String categoryName) {
        List<TagPair> result = new ArrayList<>();
        result.add(new TagPair(categoryName, "SUBJECT"));

        String text = safe(product.getName()) + " " + safe(product.getDescription());
        String[][] dictionary = {
                {"水晶", "MATERIAL"}, {"玉", "MATERIAL"}, {"铜", "MATERIAL"}, {"陶瓷", "MATERIAL"},
                {"木质", "MATERIAL"}, {"银", "MATERIAL"}, {"亚克力", "MATERIAL"}, {"羽毛", "MATERIAL"},
                {"贝壳", "MATERIAL"}, {"纸币", "MATERIAL"}, {"邮票", "MATERIAL"}, {"石", "MATERIAL"},
                {"国风", "STYLE"}, {"潮玩", "STYLE"}, {"动漫", "STYLE"}, {"复古", "STYLE"},
                {"手工", "STYLE"}, {"书法", "STYLE"}, {"油画", "STYLE"}, {"联名", "STYLE"},
                {"民国", "ERA"}, {"秦代", "ERA"}, {"奥运", "ERA"},
                {"摆件", "SCENE"}, {"装饰", "SCENE"}, {"收藏", "SCENE"}, {"礼物", "SCENE"}
        };
        Set<String> seen = new LinkedHashSet<>();
        seen.add(categoryName);
        for (String[] entry : dictionary) {
            if (result.size() >= 6) {
                break;
            }
            if (text.contains(entry[0]) && seen.add(entry[0])) {
                result.add(new TagPair(entry[0], entry[1]));
            }
        }
        return result;
    }

    /**
     * 兼容模型偶尔输出 ```json 包裹的情况。
     */
    private static String normalizeJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("模型输出为空");
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstBrace = s.indexOf('{');
            int lastBrace = s.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                s = s.substring(firstBrace, lastBrace + 1);
            }
        }
        return s;
    }

    private Map<Long, String> loadCategoryNames() {
        Map<Long, String> map = new HashMap<>();
        for (Category category : categoryMapper.selectList(null)) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private static String safe(String s) {
        return StringUtils.hasText(s) ? s : "无";
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 标签键值对
     */
    private static class TagPair {
        private final String name;
        private final String type;

        TagPair(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
