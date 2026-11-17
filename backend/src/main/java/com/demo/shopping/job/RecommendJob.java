package com.demo.shopping.job;

import com.demo.shopping.recommend.CatalogCache;
import com.demo.shopping.recommend.HotService;
import com.demo.shopping.recommend.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 推荐模块定时任务。
 *
 * 采用"近线计算"而非实时流计算：定时把行为日志聚合为热度分与用户画像，
 * 在线接口只读计算结果，保证推荐接口的响应时间稳定（毫秒级）。
 */
@Slf4j
@Component
public class RecommendJob {

    @Resource
    private HotService hotService;

    @Resource
    private ProfileService profileService;

    @Resource
    private CatalogCache catalogCache;

    /**
     * 每小时整点重算商品热度。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshHot() {
        try {
            catalogCache.refresh();
            hotService.recalc();
        } catch (Exception e) {
            log.warn("定时任务[热度重算]失败: {}", e.getMessage());
        }
    }

    /**
     * 每小时 20 分重算用户画像。
     */
    @Scheduled(cron = "0 20 * * * ?")
    public void refreshProfile() {
        try {
            catalogCache.refresh();
            profileService.rebuildAll();
        } catch (Exception e) {
            log.warn("定时任务[画像重算]失败: {}", e.getMessage());
        }
    }
}
