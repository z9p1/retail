package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.entity.Order;
import com.retail.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作台汇总数据（供 Controller 与 Agent 复用）
 */
@Service
public class WorkbenchService {

    @Autowired
    private ProductService productService;
    @Autowired
    private OrderMapper orderMapper;

    public Map<String, Object> getSummary() {
        long lowStock = productService.countLowStock(5);
        long pendingShip = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING_SHIP"));
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now();
        LocalDateTime weekStartDt = LocalDateTime.of(weekStart, LocalTime.MIN);
        long todayOrderCount = orderMapper.countPaidOrdersByTimeRange(todayStart, todayEnd);
        BigDecimal todayAmount = orderMapper.sumPaidAmountByTimeRange(todayStart, todayEnd);
        long weekOrderCount = orderMapper.countPaidOrdersByTimeRange(weekStartDt, todayEnd);
        BigDecimal weekAmount = orderMapper.sumPaidAmountByTimeRange(weekStartDt, todayEnd);
        Map<String, Object> data = new HashMap<>();
        data.put("lowStockCount", lowStock);
        data.put("pendingShipCount", pendingShip);
        data.put("zeroStockProducts", productService.listZeroStock());
        data.put("todayOrderCount", todayOrderCount);
        data.put("todayAmount", todayAmount != null ? todayAmount : BigDecimal.ZERO);
        data.put("weekOrderCount", weekOrderCount);
        data.put("weekAmount", weekAmount != null ? weekAmount : BigDecimal.ZERO);
        return data;
    }
}
