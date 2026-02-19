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
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    public Result<Order> createOrder(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
        List<?> rawItems = (List<?>) body.get("items");
        if (rawItems == null) return Result.fail(ResultCode.BAD_REQUEST, "订单商品不能为空");
        List<OrderService.OrderItemDto> items = new java.util.ArrayList<>();
        for (Object o : rawItems) {
            Map<?, ?> m = (Map<?, ?>) o;
            OrderService.OrderItemDto dto = new OrderService.OrderItemDto();
            dto.setProductId(Long.valueOf(String.valueOf(m.get("productId"))));
            dto.setQuantity(Integer.valueOf(String.valueOf(m.get("quantity"))));
            items.add(dto);
        }
        String shippingAddress = body.get("shippingAddress") != null ? body.get("shippingAddress").toString() : null;
        String idempotencyKey = request.getHeader("Idempotency-Key");
        return Result.ok(orderService.createOrder(userId, items, idempotencyKey, shippingAddress));
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
    /** 店家订单列表。userKeyword：按用户昵称或账号模糊搜索；startDate/endDate 按下单日期范围（yyyy-MM-dd）。 */
    @GetMapping("/store/orders")
    public Result<IPage<Order>> storeOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        return Result.ok(orderService.listForStore(page, size, status, userKeyword, start, end));
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

    @GetMapping("/store/orders/export")
    public void exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws java.io.IOException {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        List<Order> list = orderService.listForStoreExport(status, userKeyword, start, end);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"orders.csv\"");
        PrintWriter w = response.getWriter();
        w.write('\uFEFF');
        w.write("订单号,用户,商品汇总,金额,状态,收货地址,下单时间\n");
        for (Order o : list) {
            w.write(escapeCsv(o.getOrderNo()) + "," + escapeCsv(o.getUserDisplayName()) + "," + escapeCsv(o.getProductSummary()) + ","
                    + (o.getTotalAmount() != null ? o.getTotalAmount() : "") + "," + escapeCsv(o.getStatus()) + ","
                    + escapeCsv(o.getShippingAddress()) + "," + (o.getCreateTime() != null ? o.getCreateTime().toString() : "") + "\n");
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
