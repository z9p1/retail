package com.retail.controller;

import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.dto.QuerySemanticGateRequest;
import com.retail.dto.QuerySemanticGateResponse;
import com.retail.service.QuerySemanticGateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供 Dify 工作流通过内部 Key 调用的语义门控（原句 vs 优化句向量相似度）。
 * 鉴权：{@code X-Internal-Api-Key} 与 {@code agent.dify-internal-key} 一致，见 JwtAuthFilter。
 */
@RestController
@RequestMapping("/api/internal")
public class InternalQuerySemanticGateController {

    @Autowired
    private QuerySemanticGateService querySemanticGateService;

    @PostMapping("/query-semantic-gate")
    public Result<QuerySemanticGateResponse> gate(@RequestBody QuerySemanticGateRequest request) {
        if (request == null || !StringUtils.hasText(request.getOriginal())) {
            return Result.fail(ResultCode.BAD_REQUEST, "original 不能为空");
        }
        QuerySemanticGateResponse data = querySemanticGateService.gate(request);
        return Result.ok(data);
    }
}
