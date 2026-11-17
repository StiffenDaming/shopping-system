package com.demo.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 推荐算法参数配置。
 * 集中管理衰减系数与混合权重，便于论文实验中调整对比。
 */
@Data
@Component
@ConfigurationProperties(prefix = "recommend")
public class RecommendProperties {

    /** 时间衰减系数 λ：weight = base × exp(-λ × 天数) */
    private double decayLambda = 0.05;

    /** 热度分权重 */
    private double hotWeightOrder = 3;
    private double hotWeightCart = 2;
    private double hotWeightView = 1;

    /** 混合推荐权重：内容过滤 α、热度 β、协同过滤 γ */
    private double hybridAlpha = 0.6;
    private double hybridBeta = 0.3;
    private double hybridGamma = 0.1;

    /** 行为日志保留天数，用于画像统计窗口 */
    private int profileWindowDays = 90;

    /** 热度统计窗口天数 */
    private int hotWindowDays = 7;
}
