package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能助手单轮消息：user / assistant / system。
 */
@Data
@TableName("agent_message")
public class AgentMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联 agent_conversation.conversation_id */
    private String conversationId;
    private String role;
    private String content;
    private Integer tokenEstimate;
    private LocalDateTime createdAt;
}
