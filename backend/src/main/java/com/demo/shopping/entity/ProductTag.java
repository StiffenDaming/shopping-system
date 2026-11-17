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
 * 商品标签实体。
 * 由大模型从商品名称与描述中抽取的结构化特征，是内容过滤推荐的输入。
 */
@Data
@TableName("product_tag")
public class ProductTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String tagName;

    /** 标签类型: SUBJECT / MATERIAL / STYLE / ERA / SCENE */
    private String tagType;

    private BigDecimal weight;

    /** 来源: LLM / MANUAL / RULE */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
