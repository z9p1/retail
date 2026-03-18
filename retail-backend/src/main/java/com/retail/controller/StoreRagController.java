package com.retail.controller;

import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.service.RagService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG 知识库：店家侧触发从商品同步到向量库，供智能助手检索增强
 */
@RestController
@RequestMapping("/api/store/rag")
public class StoreRagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/ingest")
    public Result<Map<String, String>> ingest(HttpServletRequest request) {
        if (WebUtil.getUserId(request) == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }
        try {
            ragService.ingestFromProducts();
            Map<String, String> data = new HashMap<>();
            data.put("message", "已从商品表同步到 RAG 知识库");
            return Result.ok(data);
        } catch (Exception e) {
            Map<String, String> data = new HashMap<>();
            data.put("message", "同步失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            return Result.fail(ResultCode.SERVER_ERROR, data.get("message"));
        }
    }
}
