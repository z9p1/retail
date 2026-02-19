package com.retail.service;

import com.retail.entity.AccessLog;
import com.retail.mapper.AccessLogMapper;
import com.retail.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 流量监控：今日/最近7天/最近30天
 * UV 使用 Redis HyperLogLog；PV 与下单数据来自 MySQL
 */
@Service
public class TrafficService {

    private static final String UV_KEY_PREFIX = "traffic:uv:";
    private static final String CACHE_KEY_PREFIX = "traffic:cache:";

    @Value("${cache.traffic-ttl:300}")
    private int cacheTtl = 300;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AccessLogMapper accessLogMapper;
    @Autowired
    private OrderMapper orderMapper;

    /** 按天记录 UV，key=traffic:uv:yyyy-MM-dd；多日统计时用 PFMERGE 合并后 PFCOUNT */
    public void recordUv(Long userId) {
        if (userId == null) return;
        String key = UV_KEY_PREFIX + LocalDate.now().toString();
        redisTemplate.opsForHyperLogLog().add(key, userId.toString());
        redisTemplate.expire(key, 31, TimeUnit.DAYS);
    }

    public void recordPv(Long userId, String type, Long refId) {
        AccessLog log = new AccessLog();
        log.setUserId(userId);
        log.setType(type);
        log.setRefId(refId);
        log.setCreateTime(LocalDateTime.now());
        accessLogMapper.insert(log);
    }

    public Map<String, Object> getTraffic(String range) {
        String cacheKey = CACHE_KEY_PREFIX + range;
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        LocalDateTime[] rangeTime = parseRange(range);
        LocalDateTime start = rangeTime[0], end = rangeTime[1];

        // 多日 UV 用 PFMERGE 合并后 PFCOUNT，不能简单相加
        long uv = countUvInRange(start.toLocalDate(), end.toLocalDate());
        long pv = accessLogMapper.countPv(start, end);

        long orderCount = orderMapper.countPaidOrdersByTimeRange(start, end);
        long userCount = orderMapper.countDistinctUserByTimeRange(start, end);
        BigDecimal amount = orderMapper.sumPaidAmountByTimeRange(start, end);
        if (amount == null) amount = BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("uv", uv);
        result.put("pv", pv);
        result.put("orderCount", orderCount);
        result.put("userCount", userCount);
        result.put("amount", amount);
        redisTemplate.opsForValue().set(cacheKey, result, cacheTtl, TimeUnit.SECONDS);
        return result;
    }

    /** 多日 UV：将范围内每日 HLL 合并后取基数（PFMERGE + PFCOUNT） */
    private long countUvInRange(LocalDate start, LocalDate end) {
        List<String> keys = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            keys.add(UV_KEY_PREFIX + d.toString());
        }
        if (keys.isEmpty()) return 0;
        if (keys.size() == 1) {
            Long c = redisTemplate.opsForHyperLogLog().size(keys.get(0));
            return c != null ? c : 0;
        }
        // 序列化在 lambda 外；使用 String 转 byte[] 避免泛型推断问题
        byte[][] keyBytes = new byte[keys.size()][];
        for (int i = 0; i < keys.size(); i++) {
            keyBytes[i] = toBytes(keys.get(i));
        }
        String tempKeyBase = "traffic:uv:merge:" + start + ":" + end + ":" + System.currentTimeMillis();
        byte[][] tempKeyBytes = new byte[keyBytes.length][];
        for (int i = 0; i < tempKeyBytes.length; i++) {
            tempKeyBytes[i] = toBytes(tempKeyBase + ":" + i);
        }
        Long uv = redisTemplate.execute((RedisCallback<Long>) conn -> {
            conn.pfMerge(tempKeyBytes[0], keyBytes[0]);
            for (int i = 1; i < keyBytes.length; i++) {
                conn.pfMerge(tempKeyBytes[i], tempKeyBytes[i - 1], keyBytes[i]);
                conn.del(tempKeyBytes[i - 1]);
            }
            Long size = conn.pfCount(tempKeyBytes[keyBytes.length - 1]);
            conn.del(tempKeyBytes[keyBytes.length - 1]);
            return size;
        });
        return uv != null ? uv : 0;
    }

    private static byte[] toBytes(String s) {
        if (s == null) return null;
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private LocalDateTime[] parseRange(String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        if ("today".equals(range)) {
            start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        } else if ("7".equals(range)) {
            start = end.minusDays(7);
        } else {
            start = end.minusDays(30);
        }
        return new LocalDateTime[]{start, end};
    }
}
