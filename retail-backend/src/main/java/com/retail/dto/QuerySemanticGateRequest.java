package com.retail.dto;

import lombok.Data;

/**
 * Dify 工作流 HTTP 节点调用：用户原问题与 LLM 优化后问题。
 */
@Data
public class QuerySemanticGateRequest {
    /** 用户原始问题 */
    private String original;
    /** 优化/改写后的问题 */
    private String optimized;
}
