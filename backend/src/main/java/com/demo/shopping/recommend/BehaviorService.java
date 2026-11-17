package com.demo.shopping.recommend;

import com.demo.shopping.dto.BehaviorTrackDTO;
import com.demo.shopping.entity.UserBehavior;
import com.demo.shopping.mapper.UserBehaviorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户行为采集服务。
 *
 * 行为权重设计：浏览 1、加购 3、下单 5、搜索 1。
 * 加购与下单的意图强度远高于浏览，因此权重更高；
 * 搜索行为本身不绑定商品，仅记录关键词用于画像中的兴趣词。
 */
@Slf4j
@Service
public class BehaviorService {

    /** 行为类型 → 基础权重 */
    private static final Map<String, BigDecimal> WEIGHTS = new HashMap<>();

    static {
        WEIGHTS.put("VIEW", new BigDecimal("1.00"));
        WEIGHTS.put("CART", new BigDecimal("3.00"));
        WEIGHTS.put("ORDER", new BigDecimal("5.00"));
        WEIGHTS.put("SEARCH", new BigDecimal("1.00"));
    }

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    /**
     * 异步记录行为，不阻塞前端请求。
     */
    @Async("behaviorExecutor")
    public void recordAsync(Long userId, BehaviorTrackDTO dto) {
        try {
            record(userId, dto);
        } catch (Exception e) {
            // 埋点属于旁路逻辑，失败不得影响主业务
            log.warn("行为埋点写入失败: {}", e.getMessage());
        }
    }

    public boolean record(Long userId, BehaviorTrackDTO dto) {
        if (dto == null || dto.getBehaviorType() == null) {
            return false;
        }
        String type = dto.getBehaviorType().trim().toUpperCase();
        BigDecimal weight = WEIGHTS.get(type);
        if (weight == null) {
            return false;
        }
        if (!"SEARCH".equals(type) && dto.getProductId() == null) {
            return false;
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId == null ? 0L : userId);
        behavior.setProductId(dto.getProductId());
        behavior.setBehaviorType(type);
        behavior.setWeight(weight);
        behavior.setStayMs(dto.getStayMs() == null || dto.getStayMs() < 0 ? 0 : dto.getStayMs());
        behavior.setKeyword(trim(dto.getKeyword(), 100));
        behavior.setScene(trim(dto.getScene(), 50));
        userBehaviorMapper.insert(behavior);
        return true;
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
