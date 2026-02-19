package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车明细（持久化）
 */
@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 商品名称（关联查询，非表字段） */
    @TableField(exist = false)
    private String productName;
    /** 商品单价（关联查询，非表字段） */
    @TableField(exist = false)
    private BigDecimal price;
    /** 商品图片（关联查询，非表字段） */
    @TableField(exist = false)
    private String imageUrl;
}
