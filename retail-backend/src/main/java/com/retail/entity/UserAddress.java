package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收货地址（可为空，下单时可选）
 */
@Data
@TableName("user_address")
public class UserAddress {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 收货人 */
    private String receiver;
    /** 手机号 */
    private String phone;
    /** 完整地址（省市区街道门牌等） */
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
