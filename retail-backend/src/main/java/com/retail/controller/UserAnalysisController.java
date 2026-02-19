package com.retail.controller;

import com.retail.common.Result;
import com.retail.entity.User;
import com.retail.service.UserAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 店家：单用户消费分析
 */
@RestController
@RequestMapping("/api/user-analysis")
public class UserAnalysisController {

    @Autowired
    private UserAnalysisService userAnalysisService;

    /** 全部消费者列表（用于展示栏，点击姓名复写到搜索框） */
    @GetMapping("/consumers")
    public Result<List<User>> listConsumers() {
        return Result.ok(userAnalysisService.listAllConsumers());
    }

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        return Result.ok(userAnalysisService.searchUser(keyword));
    }

    @GetMapping("/{userId}")
    public Result<Map<String, Object>> get(@PathVariable Long userId) {
        return Result.ok(userAnalysisService.getAnalysis(userId));
    }
}
