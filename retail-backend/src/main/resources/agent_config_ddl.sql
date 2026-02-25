-- agent_config 表 DDL（可单独执行）
-- 用途：存储智能助手等配置，如 agent_api_key

DROP TABLE IF EXISTS agent_config;

CREATE TABLE agent_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value VARCHAR(512),
  update_time DATETIME
);
