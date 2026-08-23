package com.demo.shopping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

/**
 * 购物商城系统启动类
 */
@SpringBootApplication
@MapperScan("com.demo.shopping.mapper")
public class ShoppingApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ShoppingApplication.class, args).getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String profile = String.join(", ", env.getActiveProfiles());

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("  购物商城系统启动成功!\n");
        sb.append("  后端API: http://localhost:").append(port).append("\n");
        sb.append("  前端访问: http://localhost:5173 (需启动前端)\n");
        sb.append("  当前环境: ").append(profile).append("\n");
        sb.append("  默认账号: admin/123456 (管理员)\n");
        sb.append("            user/123456  (普通用户)\n");
        sb.append("========================================\n");
        System.out.println(sb);
    }
}
