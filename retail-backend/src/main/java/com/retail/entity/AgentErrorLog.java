package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能助手调用失败日志：真实错误进此表，对话表只落受控提示。
 */
@Data
@TableName("agent_error_log")
public class AgentErrorLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private Long userId;
    private String errorCode;
    private String errorMessage;
    private String detail;
    private LocalDateTime createdAt;
}
