package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.common.ResultCode;
import com.retail.entity.Order;
import com.retail.entity.OrderItem;
import com.retail.entity.Product;
import com.retail.entity.User;
import com.retail.exception.BusinessException;
import com.retail.mapper.OrderItemMapper;
import com.retail.mapper.OrderMapper;
import com.retail.mapper.ProductMapper;
import com.retail.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    public static final String PENDING_PAY = "PENDING_PAY";
    public static final String CANCELLED = "CANCELLED";
    public static final String PENDING_SHIP = "PENDING_SHIP";
    public static final String SHIPPED = "SHIPPED";
    public static final String COMPLETED = "COMPLETED";

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductService productService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "order:idempotency:";
    private static final int IDEMPOTENCY_TTL_SECONDS = 300;

    private static String nextOrderNo() {
        return "O" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 创建订单（仅校验库存与上下架，不扣减）。
     * 若传 idempotencyKey，相同 key 在 TTL 内重复请求返回已创建的订单，防重复提交。
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, List<OrderItemDto> items, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey.trim();
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                Order existing = orderMapper.selectById(Long.parseLong(cached.toString()));
                if (existing != null && existing.getUserId().equals(userId)) {
                    return existing;
                }
            }
        }
        if (items == null || items.isEmpty()) throw new BusinessException(ResultCode.BAD_REQUEST, "订单商品不能为空");
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDto dto : items) {
            if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "商品数量必须大于 0");
            }
            Product p = productMapper.selectById(dto.getProductId());
            if (p == null) throw new BusinessException(ResultCode.BAD_REQUEST, "商品不存在");
            if (!"ON_SALE".equals(p.getStatus())) throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF, "商品已下架");
            if (p.getStock() < dto.getQuantity()) throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "库存不足：" + p.getName());
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
            total = total.add(subtotal);
        }
        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(PENDING_PAY);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        for (OrderItemDto dto : items) {
            Product p = productMapper.selectById(dto.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(p.getId());
            item.setProductName(p.getName());
            item.setQuantity(dto.getQuantity());
            item.setPrice(p.getPrice());
            item.setSubtotal(p.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
            orderItemMapper.insert(item);
        }
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + idempotencyKey.trim(), order.getId().toString(), IDEMPOTENCY_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return order;
    }

    /** 支付：乐观锁扣减库存，更新订单状态；重复支付返回幂等 */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ResultCode.BAD_REQUEST, "订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN, "无权限");
        if (!PENDING_PAY.equals(order.getStatus())) {
            if (PENDING_SHIP.equals(order.getStatus()) || SHIPPED.equals(order.getStatus()) || COMPLETED.equals(order.getStatus())) {
                return; // 已支付，幂等返回
            }
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "订单状态不允许支付");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null || !productService.deductStock(p.getId(), p.getVersion(), item.getQuantity())) {
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "库存不足：" + (p != null ? p.getName() : ""));
            }
        }
        order.setStatus(PENDING_SHIP);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 取消订单：仅待支付可取消（文档 5.1：待发货仅可查看）；已取消则幂等返回 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ResultCode.BAD_REQUEST, "订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN, "无权限");
        if (CANCELLED.equals(order.getStatus())) return; // 幂等：已取消
        if (!PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "仅待支付订单可取消");
        }
        order.setStatus(CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 店家发货；重复发货幂等返回 */
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ResultCode.BAD_REQUEST, "订单不存在");
        if (!PENDING_SHIP.equals(order.getStatus())) {
            if (SHIPPED.equals(order.getStatus()) || COMPLETED.equals(order.getStatus())) return; // 幂等
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "仅待发货订单可发货");
        }
        order.setStatus(SHIPPED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 用户确认收货 */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ResultCode.BAD_REQUEST, "订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN, "无权限");
        if (!SHIPPED.equals(order.getStatus())) throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "仅已发货订单可确认收货");
        order.setStatus(COMPLETED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    public List<OrderItem> listItemsByOrderId(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    /** 用户：我的订单分页 */
    public IPage<Order> listByUser(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<>();
        q.eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) q.eq(Order::getStatus, status);
        q.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), q);
    }

    /** 店家：全部订单分页（填充 userDisplayName）。userKeyword 按昵称/账号/手机号模糊搜用户，不再按 userId 搜。 */
    public IPage<Order> listForStore(int page, int size, String status, String userKeyword) {
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) q.eq(Order::getStatus, status);
        if (userKeyword != null && !userKeyword.trim().isEmpty()) {
            List<Long> userIds = resolveUserIdsByKeyword(userKeyword.trim());
            if (userIds.isEmpty()) {
                q.eq(Order::getUserId, -1L); // 无匹配用户时返回空
            } else {
                q.in(Order::getUserId, userIds);
            }
        }
        q.orderByDesc(Order::getCreateTime);
        IPage<Order> result = orderMapper.selectPage(new Page<>(page, size), q);
        List<Order> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
            List<User> users = userMapper.selectBatchIds(userIds);
            java.util.Map<Long, String> nameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> (u.getNickname() != null && !u.getNickname().isEmpty()) ? u.getNickname() : u.getUsername()));
            for (Order o : records) {
                o.setUserDisplayName(nameMap.getOrDefault(o.getUserId(), "—"));
            }
            for (Order o : records) {
                List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
                if (items == null || items.isEmpty()) {
                    o.setProductSummary("—");
                    o.setTotalQuantity(0);
                } else {
                    String summary = items.stream()
                            .map(OrderItem::getProductName)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.joining("、"));
                    if (summary != null && summary.length() > 50) summary = summary.substring(0, 47) + "…";
                    o.setProductSummary(summary.isEmpty() ? "—" : summary);
                    int totalQty = items.stream().mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum();
                    o.setTotalQuantity(totalQty);
                }
            }
        }
        return result;
    }

    /** 按关键词解析用户：昵称/账号/手机号模糊匹配，若关键词为数字则同时按 id 精确匹配（如昵称是 12345 也能搜到）。 */
    private List<Long> resolveUserIdsByKeyword(String keyword) {
        LambdaQueryWrapper<User> uq = new LambdaQueryWrapper<>();
        String k = keyword;
        uq.and(w -> w.like(User::getNickname, k).or().like(User::getUsername, k).or().like(User::getPhone, k));
        try {
            Long id = Long.parseLong(k);
            uq.or(w -> w.eq(User::getId, id));
        } catch (NumberFormatException ignored) { }
        List<User> users = userMapper.selectList(uq);
        return users == null ? Collections.emptyList() : users.stream().map(User::getId).distinct().collect(Collectors.toList());
    }

    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
