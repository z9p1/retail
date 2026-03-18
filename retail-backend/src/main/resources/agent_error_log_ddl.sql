-- 智能助手调用失败记录（企业级：真实错误进此表，对话表只落受控提示）
CREATE TABLE IF NOT EXISTS agent_error_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL COMMENT 'agent_conversation.conversation_id',
  user_id BIGINT NOT NULL COMMENT '店家 user_id',
  error_code VARCHAR(32) NOT NULL DEFAULT 'DIFY_CALL_FAILED' COMMENT '错误码，便于统计与告警',
  error_message VARCHAR(512) COMMENT 'e.getMessage()',
  detail TEXT COMMENT '堆栈或请求详情，可选',
  created_at DATETIME DEFAULT NULL,
  KEY idx_user_created (user_id, created_at),
  KEY idx_conv (conversation_id)
) COMMENT '智能助手调用失败日志';
