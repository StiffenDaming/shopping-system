package com.demo.shopping.controller;

import com.demo.shopping.common.Result;
import com.demo.shopping.entity.Category;
import com.demo.shopping.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 分类列表（分类在系统初始化时预置，不提供后台维护入口）
     */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }
}
