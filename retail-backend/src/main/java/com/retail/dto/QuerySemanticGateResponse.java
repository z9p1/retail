package com.retail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义门控结果：根据余弦相似度与阈值选择下游使用的 query。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySemanticGateResponse {
    /** 余弦相似度；embedding 失败或未计算时为 null */
    private Double similarity;
    /** 下游应使用的文本：original 或 optimized */
    private String finalQuery;
    /** 是否采用了优化句（相似度不低于阈值且非降级） */
    private boolean usedOptimized;
}
