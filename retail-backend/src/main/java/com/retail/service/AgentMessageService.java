package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.entity.AgentMessage;
import com.retail.mapper.AgentMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能助手消息：按会话追加、按时间倒序查最近 N 条。高并发下最近消息列表走 Redis 缓存，写时失效。
 */
@Service
public class AgentMessageService {

    private static final String CACHE_KEY_PREFIX = "agent:ctx:list:";

    @Value("${agent.context-cache-ttl-seconds:120}")
    private long contextCacheTtlSeconds;

    @Autowired
    private AgentMessageMapper messageMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void add(String conversationId, String role, String content) {
        AgentMessage m = new AgentMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);
        invalidateCache(conversationId);
    }

    /**
     * 按时间倒序取最近 limit 条（用于拼上下文）。先查 Redis，未命中再查 DB 并回填缓存。
     */
    @SuppressWarnings("unchecked")
    public List<AgentMessage> listRecent(String conversationId, int limit) {
        if (limit <= 0) return Collections.emptyList();
        String key = CACHE_KEY_PREFIX + conversationId + ":" + limit;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                List<?> list = (List<?>) cached;
                if (!list.isEmpty() && list.get(0) instanceof AgentMessage) {
                    return (List<AgentMessage>) list;
                }
            }
        } catch (Exception ignored) { }
        List<AgentMessage> list = messageMapper.selectList(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getConversationId, conversationId)
                        .orderByDesc(AgentMessage::getCreatedAt)
                        .last("LIMIT " + limit)
        );
        List<AgentMessage> reversed = list.stream().collect(Collectors.toList());
        Collections.reverse(reversed);
        try {
            redisTemplate.opsForValue().set(key, reversed, contextCacheTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) { }
        return reversed;
    }

    private void invalidateCache(String conversationId) {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + conversationId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) { }
    }
}
