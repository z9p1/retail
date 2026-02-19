package com.retail.controller;

import com.retail.common.Result;
import com.retail.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 店家：定时任务开关（管理员功能）
 */
@RestController
@RequestMapping("/api/store/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/simulate-purchase")
    public Result<Map<String, Object>> getSimulatePurchase() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", scheduleService.isSimulatePurchaseEnabled());
        data.put("description", "customer1 每小时自动购买一件在售商品（模拟购买）");
        return Result.ok(data);
    }

    @PutMapping("/simulate-purchase")
    public Result<Void> setSimulatePurchase(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        scheduleService.setSimulatePurchaseEnabled(Boolean.TRUE.equals(enabled));
        return Result.ok();
    }
}
