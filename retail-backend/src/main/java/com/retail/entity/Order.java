package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单号（业务展示用） */
    private String orderNo;
    private Long userId;
    /** 用户姓名（昵称或账号），仅展示用，非表字段 */
    @TableField(exist = false)
    private String userDisplayName;
    /** 商品名称汇总（如「商品A, 商品B」），仅展示用，非表字段 */
    @TableField(exist = false)
    private String productSummary;
    /** 商品总件数（各明细 quantity 之和），仅展示用，非表字段 */
    @TableField(exist = false)
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    /** 状态：PENDING_PAY, CANCELLED, PENDING_SHIP, SHIPPED, COMPLETED */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime updateTime;
}
