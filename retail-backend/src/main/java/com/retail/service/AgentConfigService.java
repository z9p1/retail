package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.entity.AgentConfig;
import com.retail.mapper.AgentConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 从数据库读取 Agent 相关配置（如 API Key）。
 * Dify 多应用：dify_app_current=应用名（如 11、test1），dify_app_{应用名}=该应用的 API Key。
 */
@Service
public class AgentConfigService {

    public static final String KEY_AGENT_API_KEY = "agent_api_key";
    /** 当前使用的 Dify 应用名，对应工作室里的应用（如 11、test1） */
    public static final String KEY_DIFY_APP_CURRENT = "dify_app_current";
    /** Dify 某应用的 API Key 的 config_key 前缀，如 dify_app_11、dify_app_test1 */
    public static final String PREFIX_DIFY_APP = "dify_app_";

    @Autowired
    private AgentConfigMapper agentConfigMapper;

    public String getAgentApiKey() {
        return getConfigValue(KEY_AGENT_API_KEY);
    }

    /**
     * 获取当前应调用的 Dify 应用的 API Key。
     * 若配置了 dify_app_current，则取 dify_app_{current} 的 Key；否则退回 agent_api_key（兼容单应用）。
     */
    public String getDifyApiKey() {
        String current = getConfigValue(KEY_DIFY_APP_CURRENT);
        if (current != null && !current.isEmpty()) {
            String key = getConfigValue(PREFIX_DIFY_APP + current.trim());
            if (key != null && !key.isEmpty()) return key;
        }
        return getAgentApiKey();
    }

    /** 当前选中的 Dify 应用名（dify_app_current 的值） */
    public String getDifyAppCurrent() {
        return getConfigValue(KEY_DIFY_APP_CURRENT);
    }

    /** 所有已配置的 Dify 应用名（来自 config_key 为 dify_app_xxx 且非 dify_app_current 的键，去掉前缀） */
    public List<String> listDifyAppNames() {
        List<AgentConfig> list = agentConfigMapper.selectList(
                new LambdaQueryWrapper<AgentConfig>()
                        .likeRight(AgentConfig::getConfigKey, PREFIX_DIFY_APP)
                        .ne(AgentConfig::getConfigKey, KEY_DIFY_APP_CURRENT)
        );
        return list.stream()
                .map(AgentConfig::getConfigKey)
                .filter(k -> k != null && k.startsWith(PREFIX_DIFY_APP))
                .map(k -> k.substring(PREFIX_DIFY_APP.length()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** 设置当前使用的 Dify 应用名 */
    public void setDifyAppCurrent(String app) {
        if (app == null) app = "";
        app = app.trim();
        AgentConfig existing = agentConfigMapper.selectOne(
                new LambdaQueryWrapper<AgentConfig>().eq(AgentConfig::getConfigKey, KEY_DIFY_APP_CURRENT)
        );
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setConfigValue(app);
            existing.setUpdateTime(now);
            agentConfigMapper.updateById(existing);
        } else {
            AgentConfig c = new AgentConfig();
            c.setConfigKey(KEY_DIFY_APP_CURRENT);
            c.setConfigValue(app);
            c.setUpdateTime(now);
            agentConfigMapper.insert(c);
        }
    }

    private String getConfigValue(String configKey) {
        AgentConfig c = agentConfigMapper.selectOne(new LambdaQueryWrapper<AgentConfig>().eq(AgentConfig::getConfigKey, configKey));
        return c != null && c.getConfigValue() != null ? c.getConfigValue().trim() : null;
    }
}
