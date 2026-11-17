package com.demo.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.shopping.entity.Banner;
import com.demo.shopping.mapper.BannerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页轮播图服务
 */
@Service
public class BannerService extends ServiceImpl<BannerMapper, Banner> {

    /**
     * 前台展示用：仅返回已上架，按 sort 升序
     */
    public List<Banner> listEnabled() {
        return baseMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort)
                .orderByAsc(Banner::getId));
    }

    /**
     * 后台管理用：返回全部，按 sort 升序
     */
    public List<Banner> listAll() {
        return baseMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort)
                .orderByAsc(Banner::getId));
    }
}
