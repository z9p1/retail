package com.retail.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 定时任务开关（Redis 存储）
 */
@Service
public class ScheduleService {

    private static final String KEY_SIMULATE_PURCHASE = "retail:schedule:simulate_purchase";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean isSimulatePurchaseEnabled() {
        Object v = redisTemplate.opsForValue().get(KEY_SIMULATE_PURCHASE);
        return "1".equals(String.valueOf(v));
    }

    public void setSimulatePurchaseEnabled(boolean enabled) {
        if (enabled) {
            redisTemplate.opsForValue().set(KEY_SIMULATE_PURCHASE, "1", 365, TimeUnit.DAYS);
        } else {
            redisTemplate.delete(KEY_SIMULATE_PURCHASE);
        }
    }
}
