package com.demo.shopping.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.ProductQueryDTO;
import com.demo.shopping.entity.Product;
import com.demo.shopping.service.ProductService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商品控制器：用户浏览 + 管理员管理
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 用户端：商品分页列表（公开）
     */
    @GetMapping("/list")
    public Result<IPage<Product>> list(ProductQueryDTO query) {
        return Result.success(productService.getProductPage(query));
    }

    /**
     * 用户端：商品详情（公开）
     */
    @GetMapping("/detail/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        return Result.success(product);
    }

    // ===== 管理员功能 =====

    @GetMapping("/admin")
    public Result<IPage<Product>> adminList(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String keyword) {
        checkAdmin();
        return Result.success(productService.getAdminProductPage(page, size, keyword));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Product product) {
        checkAdmin();
        productService.addProduct(product);
        return Result.success("商品添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Product product) {
        checkAdmin();
        productService.updateProduct(product);
        return Result.success("商品修改成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        checkAdmin();
        productService.updateStatus(id, status);
        return Result.success("状态修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        productService.deleteProduct(id);
        return Result.success("商品删除成功", null);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
