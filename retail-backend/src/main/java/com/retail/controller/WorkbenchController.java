package com.retail.controller;

import com.retail.common.Result;
import com.retail.service.WorkbenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 店家：工作台概览（今日/本周关键指标、待发货数、低库存数）
 */
@RestController
@RequestMapping("/api/store/workbench")
public class WorkbenchController {

    @Autowired
    private WorkbenchService workbenchService;

    @GetMapping
    public Result<Map<String, Object>> get() {
        return Result.ok(workbenchService.getSummary());
    }
}
