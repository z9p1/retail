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
}
