package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.dto.ProductQueryDTO;
import com.demo.shopping.entity.Product;
import com.demo.shopping.mapper.ProductMapper;
import com.demo.shopping.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public IPage<Product> getProductPage(ProductQueryDTO query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Product::getName, query.getKeyword())
                    .or().like(Product::getDescription, query.getKeyword()));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }
        applySort(wrapper, query.getSort());
        return baseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
    }

    @Override
    public IPage<Product> getAdminProductPage(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void addProduct(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getPrice() == null) {
            throw new BusinessException("商品价格不能为空");
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        baseMapper.insert(product);
    }

    @Override
    public void updateProduct(Product product) {
        if (product.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        baseMapper.updateById(product);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("状态参数非法");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        baseMapper.updateById(update);
    }

    @Override
    public void deleteProduct(Long id) {
        baseMapper.deleteById(id);
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, String sort) {
        if (sort == null || sort.isEmpty()) {
            wrapper.orderByDesc(Product::getCreateTime);
            return;
        }
        switch (sort) {
            case "price-asc":
                wrapper.orderByAsc(Product::getPrice);
                break;
            case "price-desc":
                wrapper.orderByDesc(Product::getPrice);
                break;
            case "random":
                wrapper.last("ORDER BY RAND()");
                break;
            default:
                wrapper.orderByDesc(Product::getCreateTime);
        }
    }
}
