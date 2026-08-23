package com.demo.shopping.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.shopping.dto.ProductQueryDTO;
import com.demo.shopping.entity.Product;

public interface ProductService extends IService<Product> {

    /**
     * 用户端商品分页查询（仅上架）
     */
    IPage<Product> getProductPage(ProductQueryDTO query);

    /**
     * 管理端商品分页查询（含下架）
     */
    IPage<Product> getAdminProductPage(Integer page, Integer size, String keyword);

    /**
     * 新增商品
     */
    void addProduct(Product product);

    /**
     * 修改商品
     */
    void updateProduct(Product product);

    /**
     * 上架/下架商品
     */
    void updateStatus(Long id, Integer status);

    /**
     * 逻辑删除商品
     */
    void deleteProduct(Long id);
}
