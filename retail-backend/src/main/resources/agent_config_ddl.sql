-- agent_config 表 DDL（可单独执行）
-- 用途：存储智能助手等配置，如 agent_api_key

DROP TABLE IF EXISTS agent_config;

CREATE TABLE agent_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value VARCHAR(512),
  update_time DATETIME
);

-- Dify 多应用：dify_app_current=当前使用的应用名（如 11、test1），dify_app_11 / dify_app_test1 等为各应用 API Key
