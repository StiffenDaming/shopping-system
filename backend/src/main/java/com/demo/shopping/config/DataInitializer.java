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
                {"Sauce Labs Backpack", "carry.allTheThings() with the sleek, streamlined Sly Pack that melds uncompromising style with unequaled utility.", 29.99, "https://www.saucedemo.com/static/media/sauce-backpack-1200x1500.0a0b85a3.jpg", 2, 10},
                {"Sauce Labs Bike Light", "A red light isn't the desired state in testing but it sure helps when riding your bike at night.", 9.99, "https://www.saucedemo.com/static/media/bike-light-1200x1500.37c843b2.jpg", 2, 15},
                {"Sauce Labs Bolt T-Shirt", "Get your testing superhero on with the Sauce Labs bolt T-shirt.", 15.99, "https://www.saucedemo.com/static/media/bolt-shirt-1200x1500.c2599e47.jpg", 1, 20},
                {"Sauce Labs Fleece Jacket", "It's not every day that you come across a midweight quarter-zip fleece jacket.", 49.99, "https://www.saucedemo.com/static/media/sauce-pullover-1200x1500.51d275cb.jpg", 1, 8},
                {"Sauce Labs Onesie", "Ribbed snap-button bottoms and matching tank top.", 7.99, "https://www.saucedemo.com/static/media/red-onesie-1200x1500.1a4b4d48.jpg", 1, 12},
                {"Test.allTheThings() T-Shirt", "This classic Sauce Labs t-shirt is perfect to wear when cooking.", 15.99, "https://www.saucedemo.com/static/media/red-tatt-1200x1500.25bec0c9.jpg", 1, 6},
                {"Java编程思想", "Thinking in Java 第四版，经典编程入门书籍。", 89.00, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400", 3, 30},
                {"SpringBoot实战", "Spring Boot 实战派，深入浅出讲解微服务开发。", 69.00, "https://images.unsplash.com/photo-1544947950-fa07a3d50356?w=400", 3, 25},
                {"蓝牙耳机", "高保真无线蓝牙耳机，降噪主动隔离。", 199.00, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", 2, 18},
                {"机械键盘", "客制化机械键盘，青轴手感，RGB背光。", 299.00, "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400", 2, 10},
                {"跑步鞋", "轻量透气运动跑鞋，减震回弹。", 399.00, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400", 5, 14},
                {"瑜伽垫", "加厚防滑瑜伽垫，环保TPE材质。", 129.00, "https://images.unsplash.com/photo-1601925260368-ae2f43cf8b32?w=400", 5, 20},
                {"有机蓝莓", "新鲜有机蓝莓250g装，富含花青素。", 39.90, "https://images.unsplash.com/photo-1498557850926-782724f8816b?w=400", 4, 50},
                {"手冲咖啡豆", "埃塞俄比亚耶加雪菲，250g中度烘焙。", 88.00, "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=400", 4, 35},
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
