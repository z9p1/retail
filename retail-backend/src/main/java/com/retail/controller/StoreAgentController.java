package com.retail.controller;

import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.exception.BusinessException;
import com.retail.service.StoreAgentService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 店家侧智能助手：自然语言问工作台/流量数据。文本最多 20 字，30 秒限一次。
 */
@RestController
@RequestMapping("/api/store/agent")
public class StoreAgentController {

    @Autowired
    private StoreAgentService storeAgentService;

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        String message = body != null ? body.get("message") : null;
        if (message == null || message.trim().isEmpty()) {
            return Result.fail(ResultCode.BAD_REQUEST, "请输入问题");
        }
        String trimmed = message.trim();
        if (trimmed.length() > 20) {
            return Result.fail(ResultCode.BAD_REQUEST, "问题最多20个字");
        }
        try {
            String reply = storeAgentService.chat(userId, trimmed);
            Map<String, String> data = new HashMap<>();
            data.put("reply", reply);
            return Result.ok(data);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
