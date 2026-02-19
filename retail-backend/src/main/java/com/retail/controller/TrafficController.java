package com.retail.controller;

import com.retail.common.Result;
import com.retail.service.TrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 店家：流量监控（今日/7天/30天）
 */
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    @Autowired
    private TrafficService trafficService;

    @GetMapping
    public Result<Map<String, Object>> get(@RequestParam(defaultValue = "today") String range) {
        if (!"today".equals(range) && !"7".equals(range) && !"30".equals(range)) {
            range = "7";
        }
        return Result.ok(trafficService.getTraffic(range));
    }

    /** 销量趋势：近 7 天每日总金额 + 各商品每日金额，用于线型图与按商品切换 */
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {
        if (days < 1 || days > 31) days = 7;
        return Result.ok(trafficService.getSalesTrend(days));
    }
}
