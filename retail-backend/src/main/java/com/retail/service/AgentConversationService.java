package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.common.ResultCode;
import com.retail.entity.AgentConversation;
import com.retail.exception.BusinessException;
import com.retail.mapper.AgentConversationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 智能助手会话：创建/按会话ID+用户校验获取，更新 Dify conversation_id。
 * 仅允许使用本人 user_id 下的会话，他人 conversation_id 一律 403。
 */
@Service
public class AgentConversationService {

    @Autowired
    private AgentConversationMapper conversationMapper;

    /**
     * 获取或创建会话。若传入 conversationId 则校验归属 user_id 后返回；若该会话存在但属于他人则 403；否则创建新会话。
     */
    public AgentConversation getOrCreate(Long userId, String conversationIdFromFront) {
        if (StringUtils.hasText(conversationIdFromFront)) {
            AgentConversation byId = conversationMapper.selectOne(
                    new LambdaQueryWrapper<AgentConversation>()
                            .eq(AgentConversation::getConversationId, conversationIdFromFront)
            );
            if (byId != null) {
                if (!byId.getUserId().equals(userId)) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权使用该会话");
                }
                return byId;
            }
        }
        AgentConversation c = new AgentConversation();
        c.setConversationId(UUID.randomUUID().toString().replace("-", ""));
        c.setUserId(userId);
        c.setStatus("ACTIVE");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(c);
        return c;
    }

    public void updateDifyConversationId(String ourConversationId, String difyConversationId) {
        AgentConversation c = conversationMapper.selectOne(
                new LambdaQueryWrapper<AgentConversation>().eq(AgentConversation::getConversationId, ourConversationId)
        );
        if (c != null) {
            c.setDifyConversationId(difyConversationId);
            c.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(c);
        }
    }
}
