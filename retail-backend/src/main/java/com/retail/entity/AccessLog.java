package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问/行为日志（PV、趋势统计）
 */
@Data
@TableName("access_log")
public class AccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID，未登录可为空 */
    private Long userId;
    /** 类型：VIEW=浏览，ORDER=下单 */
    private String type;
    private LocalDateTime createTime;
    /** 关联ID，如订单ID */
    private Long refId;
}
