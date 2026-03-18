-- Dify 多应用：11、test1 的 API Key，当前走 test1
-- 执行：mysql -u root -p retail < retail-backend/src/main/resources/agent_config_dify_apps.sql

INSERT INTO agent_config (config_key, config_value, update_time)
VALUES ('dify_app_11', 'app-placeholder', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), update_time = NOW();

INSERT INTO agent_config (config_key, config_value, update_time)
VALUES ('dify_app_test1', 'app-placeholder', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), update_time = NOW();

INSERT INTO agent_config (config_key, config_value, update_time)
VALUES ('dify_app_current', 'test1', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), update_time = NOW();
