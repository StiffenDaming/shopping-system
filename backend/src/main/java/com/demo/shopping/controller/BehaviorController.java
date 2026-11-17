package com.demo.shopping.controller;

import com.demo.shopping.common.Result;
import com.demo.shopping.dto.BehaviorTrackDTO;
import com.demo.shopping.recommend.BehaviorService;
import com.demo.shopping.recommend.TokenResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户行为埋点接口。
 *
 * 位于拦截器白名单（游客也可上报），因此登录态由 TokenResolver 自行解析：
 * 登录用户记录真实 userId，游客记录 userId=0。
 * 写入是异步的，接口立即返回，不阻塞页面。
 */
@RestController
@RequestMapping("/api/behavior")
public class BehaviorController {

    @Resource
    private BehaviorService behaviorService;

    @Resource
    private TokenResolver tokenResolver;

    @PostMapping("/track")
    public Result<Void> track(@RequestBody BehaviorTrackDTO dto, HttpServletRequest request) {
        Long userId = tokenResolver.resolveUserId(request);
        behaviorService.recordAsync(userId, dto);
        return Result.success();
    }
}
