package com.retail.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户会话：Redis 存储，10 分钟无操作过期；异地登录挤掉旧会话。
 */
@Service
public class SessionService {

    private static final String KEY_PREFIX = "retail:session:user:";
    /** 10 分钟无操作则会话失效 */
    private static final int TTL_SECONDS = 600;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 登录时写入会话（同一用户再次登录会覆盖旧 sessionId，实现异地挤掉）。
     */
    public void saveSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null) return;
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, sessionId, TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 校验会话并刷新 TTL。无操作超过 10 分钟或异地登录则返回非 OK。
     */
    public SessionStatus validateAndRefresh(Long userId, String sessionId) {
        if (userId == null || sessionId == null) return SessionStatus.EXPIRED;
        String key = KEY_PREFIX + userId;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored == null) return SessionStatus.EXPIRED;
        if (!sessionId.equals(stored.toString())) return SessionStatus.KICKED;
        redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
        return SessionStatus.OK;
    }

    /** 返回当前 Redis 中所有在线用户 ID（有 session 键的用户）。 */
    public Set<Long> getOnlineUserIds() {
        byte[] pattern = (KEY_PREFIX + "*").getBytes(StandardCharsets.UTF_8);
        Set<byte[]> keyBytes = redisTemplate.execute((RedisCallback<Set<byte[]>>) conn -> conn.keys(pattern));
        if (keyBytes == null || keyBytes.isEmpty()) return Collections.emptySet();
        return keyBytes.stream()
                .map(b -> new String(b, StandardCharsets.UTF_8))
                .map(k -> k.startsWith(KEY_PREFIX) ? k.substring(KEY_PREFIX.length()) : null)
                .filter(id -> id != null && !id.isEmpty())
                .map(id -> {
                    try { return Long.parseLong(id); } catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    /** 踢用户下线：删除其 Redis 会话，下次请求将收到 401。 */
    public void kickUser(Long userId) {
        if (userId == null) return;
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public enum SessionStatus { OK, EXPIRED, KICKED }
}
