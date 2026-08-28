package com.demo.shopping.config;

import com.demo.shopping.entity.*;
import com.demo.shopping.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 数据初始化器：首次启动时播种测试数据
 * 账号: admin/123456 (管理员), user/123456 (普通用户)
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private UserMapper userMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private ProductMapper productMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUsers();
        initCategories();
        initProducts();
        log.info("数据初始化完成");
    }

    private void initUsers() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        String encodedPwd = passwordEncoder.encode("123456");

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(encodedPwd);
        admin.setEmail("admin@shopping.com");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        userMapper.insert(admin);

        User user = new User();
        user.setUsername("user");
        user.setPassword(encodedPwd);
        user.setEmail("user@shopping.com");
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        log.info("初始化用户数据: admin/123456, user/123456");
    }

    private void initCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }
        String[] names = {"服装", "电子产品", "图书", "食品", "运动户外"};
        for (int i = 0; i < names.length; i++) {
            Category c = new Category();
            c.setName(names[i]);
            c.setSort(i);
            categoryMapper.insert(c);
        }
        log.info("初始化商品分类: {}", String.join(", ", names));
    }

    private void initProducts() {
        if (productMapper.selectCount(null) > 0) {
            return;
        }
        Object[][] data = {
                // name, description, price, imageUrl, categoryId, stock
                {"纯棉宽松T恤", "四季百搭纯棉T恤，舒适透气，多色可选，简约休闲风格", 59.90, "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=400", 1, 50},
                {"弹力直筒牛仔裤", "经典版型弹力牛仔裤，水洗做旧工艺，修身不紧绷", 199.00, "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=400", 1, 30},
                {"连帽加绒卫衣", "秋冬加绒连帽卫衣，宽松休闲百搭，潮流街头风", 129.00, "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=400", 1, 40},
                {"无线降噪蓝牙耳机", "主动降噪技术，超长续航40小时，HiFi音质，佩戴舒适", 399.00, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", 2, 20},
                {"RGB机械键盘", "全键无冲RGB背光机械键盘，青轴/红轴可选，游戏办公皆宜", 299.00, "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400", 2, 15},
                {"智能运动手表", "心率血氧双监测，GPS定位，50米防水，多运动模式", 899.00, "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=400", 2, 10},
                {"20000mAh移动电源", "双向快充大容量移动电源，支持PD3.0/QC3.0协议，可上飞机", 159.00, "https://images.unsplash.com/photo-1609592426645-2e20f2a4c75b?w=400", 2, 25},
                {"Java编程思想（第4版）", "Bruce Eckel 经典著作，涵盖Java核心知识体系，编程入门必读", 89.00, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400", 3, 30},
                {"Spring Boot 3实战", "Spring Boot 3框架深入讲解，微服务开发实战指南", 79.00, "https://images.unsplash.com/photo-1544947950-fa07a3d50356?w=400", 3, 25},
                {"深入理解Java虚拟机", "JVM高级特性与最佳实践，深入剖析垃圾回收与内存模型", 109.00, "https://images.unsplash.com/photo-1531078085628-9f71343f9b33?w=400", 3, 20},
                {"纯牛奶250ml×16盒", "优质牧场纯牛奶，蛋白质含量3.2g/100ml，营养早餐好搭档", 49.90, "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=400", 4, 60},
                {"每日坚果礼盒", "混合坚果果干礼盒，含腰果、巴旦木、核桃、蔓越莓等", 89.00, "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?w=400", 4, 40},
                {"天然椴树蜜500g", "纯天然东北椴树蜜，结晶细腻，清香甘甜，润肺养颜", 68.00, "https://images.unsplash.com/photo-1587133568772-836045cb09a8?w=400", 4, 35},
                {"轻量缓震跑步鞋", "专业跑鞋减震回弹，轻量化设计，透气网面，适合长跑", 459.00, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400", 5, 15},
                {"TPE防滑瑜伽垫", "环保TPE材质加厚防滑瑜伽垫，高回弹，无异味", 129.00, "https://images.unsplash.com/photo-1601925260368-ae2f43cf8b32?w=400", 5, 20},
                {"全碳素羽毛球拍", "超轻碳素材质羽毛球拍，平衡点适中，攻守兼备", 329.00, "https://images.unsplash.com/photo-1511882150382-421056c89033?w=400", 5, 12},
        };

        for (Object[] row : data) {
            Product p = new Product();
            p.setName((String) row[0]);
            p.setDescription((String) row[1]);
            p.setPrice(new BigDecimal(row[2].toString()));
            p.setImageUrl((String) row[3]);
            p.setCategoryId(((Number) row[4]).longValue());
            p.setStock(((Number) row[5]).intValue());
            p.setStatus(1);
            productMapper.insert(p);
        }
        log.info("初始化商品数据: {} 件", data.length);
    }
}
