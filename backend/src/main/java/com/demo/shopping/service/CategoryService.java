package com.demo.shopping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.shopping.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listAll();
}
