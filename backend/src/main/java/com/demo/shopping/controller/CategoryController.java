package com.demo.shopping.controller;

import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
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

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Category category) {
        checkAdmin();
        categoryService.save(category);
        return Result.success("分类添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Category category) {
        checkAdmin();
        categoryService.updateById(category);
        return Result.success("分类修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        categoryService.removeById(id);
        return Result.success("分类删除成功", null);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
