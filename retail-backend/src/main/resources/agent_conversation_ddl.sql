-- 智能助手会话与消息表（Java 侧持久化，多轮对话、跨设备续聊、审计）
-- 执行：mysql -u root -p retail < retail-backend/src/main/resources/agent_conversation_ddl.sql

-- 会话表：会话级信息，归属 user_id
DROP TABLE IF EXISTS agent_message;
DROP TABLE IF EXISTS agent_conversation;

CREATE TABLE agent_conversation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务会话 ID（UUID），前端传此续聊',
  user_id BIGINT NOT NULL COMMENT '店家 user_id',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户，可选',
  dify_conversation_id VARCHAR(128) DEFAULT NULL COMMENT 'Dify 返回的 conversation_id，用于续传',
  summary VARCHAR(1024) DEFAULT NULL COMMENT '长会话摘要，用于裁剪上下文',
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_at DATETIME DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  KEY idx_user_updated (user_id, updated_at)
) COMMENT '智能助手会话';

CREATE TABLE agent_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL COMMENT '关联 agent_conversation.conversation_id',
  role VARCHAR(20) NOT NULL COMMENT 'user | assistant | system',
  content TEXT NOT NULL,
  token_estimate INT DEFAULT NULL COMMENT 'token 预估值，可选',
  created_at DATETIME DEFAULT NULL,
  KEY idx_conv_created (conversation_id, created_at)
) COMMENT '智能助手单轮消息';
