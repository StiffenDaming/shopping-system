package com.demo.shopping.controller;

import com.demo.shopping.common.Result;
import com.demo.shopping.dto.RecommendItemVO;
import com.demo.shopping.recommend.RecommendService;
import com.demo.shopping.recommend.TokenResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 推荐接口（用户端）。
 *
 * 全部位于拦截器白名单：未登录的游客同样可以浏览热门榜与相似藏品；
 * "为你推荐"在未登录时自动退化为热度榜（冷启动策略）。
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    @Resource
    private TokenResolver tokenResolver;

    /**
     * 热门藏品榜
     */
    @GetMapping("/hot")
    public Result<List<RecommendItemVO>> hot(@RequestParam(required = false) Long categoryId,
                                             @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(recommendService.hot(categoryId, clamp(limit)));
    }

    /**
     * 相似藏品（内容过滤）
     */
    @GetMapping("/similar/{productId}")
    public Result<List<RecommendItemVO>> similar(@PathVariable Long productId,
                                                 @RequestParam(defaultValue = "6") Integer limit) {
        return Result.success(recommendService.similar(productId, clamp(limit)));
    }

    /**
     * 为你推荐（混合加权，未登录自动退化为热度榜）
     */
    @GetMapping("/for-you")
    public Result<List<RecommendItemVO>> forYou(HttpServletRequest request,
                                                @RequestParam(defaultValue = "8") Integer limit) {
        Long userId = tokenResolver.resolveUserId(request);
        return Result.success(recommendService.forYou(userId, clamp(limit)));
    }

    private int clamp(Integer limit) {
        if (limit == null || limit < 1) {
            return 8;
        }
        return Math.min(limit, 50);
    }
}
