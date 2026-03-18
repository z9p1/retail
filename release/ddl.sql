-- ============================================================
-- 店家线上零售系统 DDL（MySQL 8.x）先删后增，本次发布
-- 执行：mysql -u root -p < release/ddl.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS retail DEFAULT CHARACTER SET utf8mb4;
USE retail;

-- ----------------------------------------
-- 先删：按子表 -> 父表顺序 DROP
-- ----------------------------------------
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS access_log;
DROP TABLE IF EXISTS agent_error_log;
DROP TABLE IF EXISTS agent_message;
DROP TABLE IF EXISTS agent_conversation;
DROP TABLE IF EXISTS agent_config;
DROP TABLE IF EXISTS rag_chunk;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS sys_user;

-- ----------------------------------------
-- 后增：建表
-- ----------------------------------------
-- 用户（店家/客户）
CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(64) NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  nickname VARCHAR(64),
  phone VARCHAR(32),
  create_time DATETIME,
  update_time DATETIME
);

-- 商品（含 version 乐观锁）
CREATE TABLE product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE',
  description VARCHAR(512),
  image_url VARCHAR(256),
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

-- 订单
CREATE TABLE `order` (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(24) NOT NULL,
  shipping_address VARCHAR(512),
  create_time DATETIME,
  pay_time DATETIME,
  update_time DATETIME
);

-- 订单明细
CREATE TABLE order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(128),
  quantity INT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL
);

-- 购物车明细（持久化，按用户+商品唯一）
CREATE TABLE cart_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_user_product (user_id, product_id)
);

-- 用户收货地址（可为空，下单时可选）
CREATE TABLE user_address (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(512) NOT NULL,
  create_time DATETIME,
  update_time DATETIME
);

-- 系统配置（智能助手 API Key、Dify 多应用等）
CREATE TABLE agent_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value VARCHAR(512),
  update_time DATETIME
);

-- 智能助手会话（Java 侧持久化，多轮续聊、跨设备、审计）
CREATE TABLE agent_conversation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务会话 ID（UUID），前端传此续聊',
  user_id BIGINT NOT NULL COMMENT '店家 user_id',
  tenant_id BIGINT DEFAULT NULL,
  dify_conversation_id VARCHAR(128) DEFAULT NULL COMMENT 'Dify 返回的 conversation_id',
  summary VARCHAR(1024) DEFAULT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_at DATETIME DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  KEY idx_user_updated (user_id, updated_at)
) COMMENT '智能助手会话';

-- 智能助手消息（关联 agent_conversation.conversation_id）
CREATE TABLE agent_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL COMMENT '关联 agent_conversation.conversation_id',
  role VARCHAR(20) NOT NULL COMMENT 'user | assistant | system',
  content TEXT NOT NULL,
  token_estimate INT DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  KEY idx_conv_created (conversation_id, created_at)
) COMMENT '智能助手单轮消息';

-- 智能助手调用失败日志（真实错误进此表，对话表只落受控提示）
CREATE TABLE agent_error_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL COMMENT 'agent_conversation.conversation_id',
  user_id BIGINT NOT NULL COMMENT '店家 user_id',
  error_code VARCHAR(32) NOT NULL DEFAULT 'DIFY_CALL_FAILED',
  error_message VARCHAR(512),
  detail TEXT COMMENT '堆栈或请求详情',
  created_at DATETIME DEFAULT NULL,
  KEY idx_user_created (user_id, created_at),
  KEY idx_conv (conversation_id)
) COMMENT '智能助手调用失败日志';

-- RAG 知识库：文本块与向量（embedding 存 JSON 数组）
CREATE TABLE rag_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content TEXT NOT NULL COMMENT '文本块内容',
  embedding_json TEXT NOT NULL COMMENT '向量 JSON，如 [0.1,-0.2,...]',
  source_type VARCHAR(32) NOT NULL DEFAULT 'product' COMMENT '来源：product, faq, doc',
  source_id VARCHAR(64) COMMENT '来源 ID，如商品 id',
  create_time DATETIME
);
CREATE INDEX idx_rag_chunk_source ON rag_chunk(source_type, source_id);

-- 访问/行为日志
CREATE TABLE access_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  type VARCHAR(16),
  create_time DATETIME,
  ref_id BIGINT
);
