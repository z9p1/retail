package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（店家 / 普通用户）
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 登录账号 */
    private String username;
    /** 密码（加密存储） */
    private String password;
    /** 角色：STORE=店家，USER=用户 */
    private String role;
    /** 昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
