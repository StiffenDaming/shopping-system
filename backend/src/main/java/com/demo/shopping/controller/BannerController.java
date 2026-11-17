package com.demo.shopping.controller;

import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.entity.Banner;
import com.demo.shopping.service.BannerService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 首页轮播图控制器。
 *
 * 前台只需只读列表；后台提供增删改查。
 */
@RestController
public class BannerController {

    @Resource
    private BannerService bannerService;

    /**
     * 前台：已上架的轮播图（无需登录）
     */
    @GetMapping("/api/banners/list")
    public Result<List<Banner>> list() {
        return Result.success(bannerService.listEnabled());
    }

    /**
     * 后台：全部轮播图
     */
    @GetMapping("/api/admin/banners")
    public Result<List<Banner>> adminList() {
        checkAdmin();
        return Result.success(bannerService.listAll());
    }

    @PostMapping("/api/admin/banners")
    public Result<Void> add(@RequestBody Banner banner) {
        checkAdmin();
        validate(banner);
        banner.setId(null);
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        bannerService.save(banner);
        return Result.success("轮播图添加成功", null);
    }

    @PutMapping("/api/admin/banners")
    public Result<Void> update(@RequestBody Banner banner) {
        checkAdmin();
        if (banner.getId() == null) {
            throw new BusinessException("轮播图ID不能为空");
        }
        validate(banner);
        bannerService.updateById(banner);
        return Result.success("轮播图修改成功", null);
    }

    @DeleteMapping("/api/admin/banners/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        bannerService.removeById(id);
        return Result.success("轮播图删除成功", null);
    }

    private void validate(Banner banner) {
        if (!StringUtils.hasText(banner.getImageUrl())) {
            throw new BusinessException("图片地址不能为空");
        }
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
