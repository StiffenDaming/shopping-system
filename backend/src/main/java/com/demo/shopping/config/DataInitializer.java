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
        String[] names = {
                "文玩雅器",          // 1
                "古玩雕像",          // 2
                "书刊影集",          // 3
                "钱币票证",          // 4
                "潮玩盲盒",          // 5
                "动漫游戏周边",      // 6
                "镇馆之宝"           // 7
        };
        for (int i = 0; i < names.length; i++) {
            Category c = new Category();
            c.setName(names[i]);
            c.setSort(i);
            categoryMapper.insert(c);
        }
        log.info("初始化收藏品分类: {}", String.join(", ", names));
    }

    private void initProducts() {
        if (productMapper.selectCount(null) > 0) {
            return;
        }
        Object[][] data = {
                // ===== 文玩雅器 (categoryId=1) =====
                {"100种天然水晶矿石", "包含紫水晶、黄水晶、萤石等，标本级，带展示盒", 20.40, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%96%87%E7%8E%A9%E9%9B%85%E5%99%A8/crystals.jpg", 1, 18},
                {"亚马逊风铃", "南美风格手工风铃，天然羽毛＋铜管，清越悦耳", 39.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%96%87%E7%8E%A9%E9%9B%85%E5%99%A8/windchime.jpg", 1, 10},
                {"李太白真迹", "古代书法作品（高仿），卷轴装裱，可作装饰", 299.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%96%87%E7%8E%A9%E9%9B%85%E5%99%A8/libai.jpg", 1, 2},
                {"国风墨韵中式圆盘", "陶瓷圆形摆件，水墨风格，桌面装饰品", 49.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%96%87%E7%8E%A9%E9%9B%85%E5%99%A8/plate.jpg", 1, 15},
                {"天然贝壳标本", "南海稀有贝壳，完整无损，带支架底座", 39.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%96%87%E7%8E%A9%E9%9B%85%E5%99%A8/shell.jpg", 1, 7},

                // ===== 古玩雕像 (categoryId=2) =====
                {"【清仓价】正品碧海灵龙", "清仓价，原价1299元，现仅售899元，限量雕像", 899.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E5%8F%A4%E7%8E%A9%E9%9B%95%E5%83%8F/bihai.jpg", 2, 8},
                {"思考者【砂岩款】", "经典罗丹《思想者》复刻雕塑，砂岩材质，质感厚重，书房/客厅艺术摆件", 329.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E5%8F%A4%E7%8E%A9%E9%9B%95%E5%83%8F/thinker.jpg", 2, 3},
                {"实心纯铜李小龙铜像", "实心纯铜，高约30cm，经典截拳道姿势，收藏佳品", 499.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E5%8F%A4%E7%8E%A9%E9%9B%95%E5%83%8F/brucelee.jpg", 2, 2},

                // ===== 书刊影集 (categoryId=3) =====
                {"绝版书籍【限定出售】《我的奋斗》", "特殊历史时期版本，稀见文献，仅此五本", 299.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E4%B9%A6%E5%88%8A%E5%BD%B1%E9%9B%86/meinkampf.jpg", 3, 5},
                {"巴黎奥运40金相册集", "2024巴黎奥运会中国代表团40枚金牌全纪录，高清纪实影像，含开幕式及闭幕式精彩瞬间", 199.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E4%B9%A6%E5%88%8A%E5%BD%B1%E9%9B%86/paris-olympic-40.jpg", 3, 1},
                {"《山海经全彩》", "全彩插图版，收录珍稀古本图像，硬壳精装", 159.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E4%B9%A6%E5%88%8A%E5%BD%B1%E9%9B%86/shanhaijing.jpg", 3, 12},

                // ===== 钱币票证 (categoryId=4) =====
                {"火影忍者纪念钞", "非流通纪念钞，火影忍者官方授权，含收藏卡", 9.88, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E9%92%B1%E5%B8%81%E7%A5%A8%E8%AF%81/naruto-bill.jpg", 4, 30},
                {"十二生肖邮票", "十二生肖大全套，含收藏册，品相完好", 199.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E9%92%B1%E5%B8%81%E7%A5%A8%E8%AF%81/stamp-12.jpg", 4, 6},
                {"民国纸币", "民国时期真品纸币，品相七成新，带展示卡", 89.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E9%92%B1%E5%B8%81%E7%A5%A8%E8%AF%81/republic-bill.jpg", 4, 4},

                // ===== 潮玩盲盒 (categoryId=5) =====
                {"奶龙-神明降临", "油画风格质感，神明降临系列，含隐藏款", 89.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%BD%AE%E7%8E%A9%E7%9B%B2%E7%9B%92/nailong.jpg", 5, 24},
                {"入酒虫盲盒玩偶", "王者荣耀系列联名盲盒，随机款，含隐藏", 59.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E6%BD%AE%E7%8E%A9%E7%9B%B2%E7%9B%92/rujiu.jpg", 5, 36},

                // ===== 动漫游戏周边 (categoryId=6) =====
                {"哥伦比娅水晶球", "高透水晶球，内含微缩场景，直径约10cm", 19.89, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E5%8A%A8%E6%BC%AB%E6%B8%B8%E6%88%8F%E5%91%A8%E8%BE%B9/crystalball.jpg", 6, 15},
                {"亚力克相框 【鸣潮卡提希娅】", "角色限定亚克力相框，双面印刷，含支架", 49.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E5%8A%A8%E6%BC%AB%E6%B8%B8%E6%88%8F%E5%91%A8%E8%BE%B9/frame.jpg", 6, 20},

                // ===== 镇馆之宝 (categoryId=7) =====
                {"传国玉玺", "高仿古玉玺，仿制秦代传国玉玺，雕刻精细，含锦盒", 9999.00, "https://shopping-system.oss-cn-guangzhou.aliyuncs.com/%E9%95%87%E9%A6%86%E4%B9%8B%E5%AE%9D/seal.jpg", 7, 1},
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
        log.info("初始化收藏品数据: {} 件", data.length);
    }
}