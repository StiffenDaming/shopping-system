package com.demo.shopping.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.config.RecommendProperties;
import com.demo.shopping.entity.UserBehavior;
import com.demo.shopping.entity.UserProfile;
import com.demo.shopping.entity.User;
import com.demo.shopping.mapper.UserBehaviorMapper;
import com.demo.shopping.mapper.UserMapper;
import com.demo.shopping.mapper.UserProfileMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户画像构建服务。
 *
 * 核心思想：用户对某个标签的兴趣强度，等于他所有相关行为的加权累加，
 * 但越久远的行为参考价值越低，因此用指数衰减 exp(-λ × 天数) 做时间折扣。
 * 这样画像能自动"遗忘"过时兴趣，比简单计数更贴近真实偏好。
 */
@Slf4j
@Service
public class ProfileService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private RecommendProperties properties;

    @Resource
    private CatalogCache catalogCache;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 重算全部用户画像。
     */
    public Map<String, Object> rebuildAll() {
        List<User> users = userMapper.selectList(null);
        int ok = 0;
        int empty = 0;
        for (User user : users) {
            try {
                if (rebuild(user.getId())) {
                    ok++;
                } else {
                    empty++;
                }
            } catch (Exception e) {
                log.warn("用户[{}]画像构建失败: {}", user.getId(), e.getMessage());
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("users", users.size());
        summary.put("built", ok);
        summary.put("noBehavior", empty);
        log.info("用户画像重建完成: {}", summary);
        return summary;
    }

    /**
     * 重算单个用户画像。
     *
     * @return false 表示该用户没有任何行为，画像为空
     */
    public boolean rebuild(Long userId) {
        if (userId == null) {
            return false;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(properties.getProfileWindowDays());
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .ge(UserBehavior::getCreateTime, since));

        Map<String, Double> tagVector = new HashMap<>();
        Map<String, Double> categoryVector = new HashMap<>();
        double priceSum = 0;
        double weightSum = 0;
        int viewCnt = 0;
        int cartCnt = 0;
        int orderCnt = 0;

        LocalDateTime now = LocalDateTime.now();
        for (UserBehavior behavior : behaviors) {
            if ("VIEW".equals(behavior.getBehaviorType())) {
                viewCnt++;
            } else if ("CART".equals(behavior.getBehaviorType())) {
                cartCnt++;
            } else if ("ORDER".equals(behavior.getBehaviorType())) {
                orderCnt++;
            }

            Long productId = behavior.getProductId();
            if (productId == null) {
                continue;
            }
            double baseWeight = behavior.getWeight() == null ? 1.0 : behavior.getWeight().doubleValue();
            double decay = decayOf(behavior.getCreateTime(), now);
            double weight = baseWeight * decay;

            Long categoryId = catalogCache.categoryOf(productId);
            if (categoryId != null) {
                categoryVector.merge("CAT:" + categoryId, weight, Double::sum);
                String categoryName = catalogCache.categoryName(categoryId);
                if (categoryName != null) {
                    tagVector.merge(categoryName, weight, Double::sum);
                }
            }
            for (String tag : catalogCache.tagsOf(productId)) {
                tagVector.merge(tag, weight, Double::sum);
            }

            BigDecimal price = catalogCache.priceOf(productId);
            if (price != null) {
                priceSum += weight * price.doubleValue();
                weightSum += weight;
            }
        }

        if (behaviors.isEmpty()) {
            return false;
        }

        normalize(tagVector);
        normalize(categoryVector);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setTagVector(toJson(tagVector));
        profile.setCategoryVector(toJson(categoryVector));
        profile.setPricePref(weightSum > 0
                ? BigDecimal.valueOf(priceSum / weightSum).setScale(2, RoundingMode.HALF_UP)
                : null);
        profile.setViewCnt(viewCnt);
        profile.setCartCnt(cartCnt);
        profile.setOrderCnt(orderCnt);
        profile.setActivityLevel(activityLevel(behaviors.size()));

        saveOrUpdate(profile);
        return true;
    }

    /**
     * 读取画像。
     */
    public UserProfile profileOf(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
    }

    /**
     * 解析画像中的标签向量。
     */
    public Map<String, Double> parseVector(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Map<String, Double> map = objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {
            });
            return map == null ? new HashMap<>() : map;
        } catch (Exception e) {
            log.warn("画像向量解析失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private void saveOrUpdate(UserProfile profile) {
        UserProfile existing = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, profile.getUserId()));
        if (existing == null) {
            userProfileMapper.insert(profile);
        } else {
            profile.setId(existing.getId());
            userProfileMapper.updateById(profile);
        }
    }

    /**
     * 指数时间衰减：exp(-λ × 天数)。
     */
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

    /**
     * 归一化到 [0,1]，以最大值为基准，保留相对强弱。
     */
    private void normalize(Map<String, Double> vector) {
        if (vector.isEmpty()) {
            return;
        }
        double max = 0;
        for (double v : vector.values()) {
            max = Math.max(max, v);
        }
        if (max <= 0) {
            return;
        }
        List<String> keys = new ArrayList<>(vector.keySet());
        for (String key : keys) {
            vector.put(key, round(vector.get(key) / max));
        }
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static String activityLevel(int behaviorCount) {
        if (behaviorCount >= 30) {
            return "HIGH";
        }
        if (behaviorCount >= 10) {
            return "MID";
        }
        return "LOW";
    }

    private String toJson(Map<String, Double> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
