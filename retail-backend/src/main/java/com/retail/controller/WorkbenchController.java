package com.retail.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.common.Result;
import com.retail.entity.Order;
import com.retail.mapper.OrderMapper;
import com.retail.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 店家：工作台概览（今日指标、待发货数、低库存数）
 */
@RestController
@RequestMapping("/api/store/workbench")
public class WorkbenchController {

    @Autowired
    private ProductService productService;
    @Autowired
    private OrderMapper orderMapper;

    @GetMapping
    public Result<Map<String, Object>> get() {
        long lowStock = productService.countLowStock(5);
        long pendingShip = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING_SHIP"));
        Map<String, Object> data = new HashMap<>();
        data.put("lowStockCount", lowStock);
        data.put("pendingShipCount", pendingShip);
        data.put("zeroStockProducts", productService.listZeroStock());
        return Result.ok(data);
    }
}
