package com.demo.shopping.controller;

import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.AssistantAnswerVO;
import com.demo.shopping.dto.AssistantAskDTO;
import com.demo.shopping.dto.FaqItemVO;
import com.demo.shopping.service.AssistantService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 客服助手接口。
 *
 * 用户端：推荐问题 + 提问
 * 管理端：查看未命中知识库的高频问题（用于补充 FAQ）
 */
@RestController
public class AssistantController {

    @Resource
    private AssistantService assistantService;

    /**
     * 推荐问题（前端作为快捷提问按钮）
     */
    @GetMapping("/api/assistant/faq")
    public Result<List<FaqItemVO>> faq() {
        return Result.success(assistantService.listFaqItems());
    }

    /**
     * 提问：先查知识库，未命中再调用大模型
     */
    @PostMapping("/api/assistant/chat")
    public Result<AssistantAnswerVO> chat(@Valid @RequestBody AssistantAskDTO dto) {
        return Result.success(assistantService.ask(UserContext.getCurrentId(), dto.getQuestion()));
    }

    /**
     * 管理端：未命中知识库的高频问题
     */
    @GetMapping("/api/admin/assistant/unanswered")
    public Result<List<Map<String, Object>>> unanswered(
            @RequestParam(defaultValue = "20") int limit) {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
        return Result.success(assistantService.topUnanswered(limit));
    }
}
