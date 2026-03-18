package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能助手会话：会话级信息，归属 user_id，与 Dify 的 conversation 可关联。
 */
@Data
@TableName("agent_conversation")
public class AgentConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务会话 ID（UUID），前端传此续聊 */
    private String conversationId;
    private Long userId;
    private Long tenantId;
    /** Dify 返回的 conversation_id，用于续传 */
    private String difyConversationId;
    /** 长会话摘要，用于裁剪上下文 */
    private String summary;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
