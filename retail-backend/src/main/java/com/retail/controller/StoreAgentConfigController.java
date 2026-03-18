package com.retail.controller;

import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.service.AgentConfigService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店家：智能助手配置（如当前使用的 Dify 应用）
 */
@RestController
@RequestMapping("/api/store/agent-config")
public class StoreAgentConfigController {

    @Autowired
    private AgentConfigService agentConfigService;

    @GetMapping("/dify-app")
    public Result<Map<String, Object>> getDifyApp(HttpServletRequest request) {
        if (WebUtil.getUserId(request) == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }
        List<String> options = agentConfigService.listDifyAppNames();
        String current = agentConfigService.getDifyAppCurrent();
        Map<String, Object> data = new HashMap<>();
        data.put("options", options);
        data.put("current", current != null ? current : "");
        return Result.ok(data);
    }

    @PutMapping("/dify-app")
    public Result<Void> setDifyApp(HttpServletRequest request, @RequestBody Map<String, String> body) {
        if (WebUtil.getUserId(request) == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }
        String app = body != null ? body.get("app") : null;
        agentConfigService.setDifyAppCurrent(app);
        return Result.ok();
    }
}
