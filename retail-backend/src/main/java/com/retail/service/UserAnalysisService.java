package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.entity.Order;
import com.retail.entity.OrderItem;
import com.retail.entity.User;
import com.retail.mapper.OrderItemMapper;
import com.retail.mapper.OrderMapper;
import com.retail.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 单用户消费分析，结果 Redis 缓存 5 分钟
 */
@Service
public class UserAnalysisService {

    private static final String CACHE_KEY_PREFIX = "user:analysis:";

    @Value("${cache.user-analysis-ttl:300}")
    private int cacheTtl = 300;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 查询所有消费者（角色 USER），用于展示栏 */
    public List<User> listAllConsumers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "USER").orderByAsc(User::getId));
    }

    /** 按用户ID、手机号或昵称搜索用户 */
    public List<User> searchUser(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Collections.emptyList();
        String k = keyword.trim();
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        try {
            Long id = Long.parseLong(k);
            q.eq(User::getId, id);
        } catch (NumberFormatException ignored) {
            q.and(w -> w.like(User::getPhone, k).or().like(User::getNickname, k).or().like(User::getUsername, k));
        }
        return userMapper.selectList(q);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAnalysis(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        User user = userMapper.selectById(userId);
        if (user == null) return Collections.emptyMap();

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .in(Order::getStatus, OrderService.PENDING_SHIP, OrderService.SHIPPED, OrderService.COMPLETED)
                        .orderByAsc(Order::getPayTime)
        );
        long orderCount = orders.size();
        BigDecimal totalAmount = orders.stream().map(Order::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime firstPay = orders.isEmpty() ? null : orders.get(0).getPayTime();
        LocalDateTime lastPay = orders.isEmpty() ? null : orders.get(orders.size() - 1).getPayTime();

        Map<Long, Map<String, Object>> productPref = new HashMap<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            for (OrderItem item : items) {
                productPref.computeIfAbsent(item.getProductId(), k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", item.getProductId());
                    m.put("productName", item.getProductName());
                    m.put("quantity", 0);
                    m.put("amount", BigDecimal.ZERO);
                    return m;
                });
                Map<String, Object> m = productPref.get(item.getProductId());
                m.put("quantity", (Integer) m.get("quantity") + item.getQuantity());
                m.put("amount", ((BigDecimal) m.get("amount")).add(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO));
            }
        }
        List<Map<String, Object>> preference = productPref.values().stream()
                .sorted((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")))
                .limit(20)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("orderCount", orderCount);
        result.put("totalAmount", totalAmount);
        result.put("firstPayTime", firstPay);
        result.put("lastPayTime", lastPay);
        result.put("orders", orders); // 文档 4.4：订单明细列表（时间、订单号、金额、状态）
        result.put("preference", preference);
        redisTemplate.opsForValue().set(cacheKey, result, cacheTtl, TimeUnit.SECONDS);
        return result;
    }
}
