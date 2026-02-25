package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.entity.AgentConfig;
import com.retail.mapper.AgentConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 从数据库读取 Agent 相关配置（如 API Key）
 */
@Service
public class AgentConfigService {

    public static final String KEY_AGENT_API_KEY = "agent_api_key";

    @Autowired
    private AgentConfigMapper agentConfigMapper;

    public String getAgentApiKey() {
        AgentConfig c = agentConfigMapper.selectOne(new LambdaQueryWrapper<AgentConfig>().eq(AgentConfig::getConfigKey, KEY_AGENT_API_KEY));
        return c != null && c.getConfigValue() != null ? c.getConfigValue().trim() : null;
    }
}
