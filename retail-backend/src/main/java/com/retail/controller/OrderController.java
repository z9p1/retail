package com.retail.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.entity.Order;
import com.retail.entity.OrderItem;
import com.retail.service.OrderService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户订单：创建、支付、取消、确认收货
 * 店家订单：列表、发货、详情
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ---------- 用户 ----------
    @PostMapping("/user/orders")
    public Result<Order> createOrder(HttpServletRequest request, @RequestBody List<OrderService.OrderItemDto> items) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        String idempotencyKey = request.getHeader("Idempotency-Key");
        return Result.ok(orderService.createOrder(userId, items, idempotencyKey));
    }

    @PostMapping("/user/orders/{orderId}/pay")
    public Result<Void> pay(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        orderService.pay(userId, orderId);
        return Result.ok();
    }

    @PostMapping("/user/orders/{orderId}/cancel")
    public Result<Void> cancel(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        orderService.cancel(userId, orderId);
        return Result.ok();
    }

    @PostMapping("/user/orders/{orderId}/confirm")
    public Result<Void> confirm(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        orderService.confirm(userId, orderId);
        return Result.ok();
    }

    @GetMapping("/user/orders")
    public Result<IPage<Order>> myOrders(HttpServletRequest request,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String status) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        return Result.ok(orderService.listByUser(userId, page, size, status));
    }

    @GetMapping("/user/orders/{orderId}")
    public Result<Map<String, Object>> orderDetail(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        Order order = orderService.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) return Result.fail(ResultCode.FORBIDDEN, "无权限");
        List<OrderItem> items = orderService.listItemsByOrderId(orderId);
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return Result.ok(data);
    }

    // ---------- 店家 ----------
    /** 店家订单列表。userKeyword：按用户昵称或账号模糊搜索，不再按 userId 精确匹配。 */
    @GetMapping("/store/orders")
    public Result<IPage<Order>> storeOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userKeyword) {
        return Result.ok(orderService.listForStore(page, size, status, userKeyword));
    }

    @GetMapping("/store/orders/{orderId}")
    public Result<Map<String, Object>> storeOrderDetail(@PathVariable Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return Result.fail(ResultCode.BAD_REQUEST, "订单不存在");
        List<OrderItem> items = orderService.listItemsByOrderId(orderId);
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return Result.ok(data);
    }

    @PostMapping("/store/orders/{orderId}/ship")
    public Result<Void> ship(@PathVariable Long orderId) {
        orderService.ship(orderId);
        return Result.ok();
    }
}
