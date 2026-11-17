package com.demo.shopping.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户画像实体。
 * 由用户行为日志按时间衰减聚合而成，保存标签偏好向量与分类偏好向量。
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 标签偏好向量 JSON: {"标签名": 权重} */
    private String tagVector;

    /** 分类偏好向量 JSON: {"分类ID": 权重} */
    private String categoryVector;

    /** 价格偏好（按行为加权的平均价格） */
    private BigDecimal pricePref;

    /** 活跃度: HIGH / MID / LOW */
    private String activityLevel;

    private Integer viewCnt;

    private Integer cartCnt;

    private Integer orderCnt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
