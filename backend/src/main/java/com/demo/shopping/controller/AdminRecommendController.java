package com.demo.shopping.controller;

import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.ProfileVO;
import com.demo.shopping.recommend.CatalogCache;
import com.demo.shopping.recommend.HotService;
import com.demo.shopping.recommend.LlmClient;
import com.demo.shopping.recommend.ProfileQueryService;
import com.demo.shopping.recommend.ProfileService;
import com.demo.shopping.recommend.TagExtractionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐模块管理接口（仅管理员）。
 *
 * 提供标签抽取、画像/热度重算与画像查看，方便论文实验与演示时手动触发。
 */
@RestController
@RequestMapping("/api/admin/recommend")
public class AdminRecommendController {

    @Resource
    private TagExtractionService tagExtractionService;

    @Resource
    private HotService hotService;

    @Resource
    private ProfileService profileService;

    @Resource
    private ProfileQueryService profileQueryService;

    @Resource
    private CatalogCache catalogCache;

    @Resource
    private LlmClient llmClient;

    /**
     * 用户画像列表
     */
    @GetMapping("/profiles")
    public Result<List<ProfileVO>> profiles() {
        checkAdmin();
        return Result.success(profileQueryService.list());
    }

    /**
     * 单个用户画像详情
     */
    @GetMapping("/profile/{userId}")
    public Result<ProfileVO> profile(@PathVariable Long userId) {
        checkAdmin();
        ProfileVO vo = profileQueryService.detail(userId);
        if (vo == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(vo);
    }

    /**
     * 商品标签抽取（大模型落点 L1）
     *
     * @param force true=全部重新抽取；false=只处理还没有标签的商品
     */
    @PostMapping("/extract-tags")
    public Result<Map<String, Object>> extractTags(
            @RequestParam(defaultValue = "false") boolean force) {
        checkAdmin();
        return Result.success(tagExtractionService.extractAll(force));
    }

    /**
     * 重算热度榜 + 用户画像
     */
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild() {
        checkAdmin();
        catalogCache.refresh();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hot", hotService.recalc());
        result.put("profile", profileService.rebuildAll());
        return Result.success(result);
    }

    /**
     * 推荐模块运行状态，便于排查"为什么没有推荐"
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        checkAdmin();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("llmEnabled", llmClient.isEnabled());
        result.put("productCount", catalogCache.allProducts().size());
        return Result.success(result);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
