package com.retail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能助手单次对话返回：回复内容 + 会话 ID（Java 侧持久化时的业务会话 ID，用于多轮续聊）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreAgentChatResult {
    private String reply;
    private String conversationId;
}
