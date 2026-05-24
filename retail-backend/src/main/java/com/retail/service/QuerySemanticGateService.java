package com.retail.service;

import com.retail.dto.QuerySemanticGateRequest;
import com.retail.dto.QuerySemanticGateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 自然语言优化校验：原句与优化句 embedding 余弦相似度低于阈值则回退原句。
 */
@Service
public class QuerySemanticGateService {

    private static final Logger log = LoggerFactory.getLogger(QuerySemanticGateService.class);

    @Value("${agent.query-optimize-similarity-threshold:0.8}")
    private double similarityThreshold;

    @Autowired
    private SemanticSimilarityService semanticSimilarityService;

    public QuerySemanticGateResponse gate(QuerySemanticGateRequest req) {
        String original = req != null && req.getOriginal() != null ? req.getOriginal().trim() : "";
        String optimized = req != null && req.getOptimized() != null ? req.getOptimized().trim() : "";

        if (!StringUtils.hasText(original)) {
            return new QuerySemanticGateResponse(null, "", false);
        }
        if (!StringUtils.hasText(optimized)) {
            log.debug("query semantic gate: empty optimized, using original");
            return new QuerySemanticGateResponse(null, original, false);
        }
        if (original.equals(optimized)) {
            return new QuerySemanticGateResponse(1.0, original, true);
        }

        Double sim = semanticSimilarityService.similarityBetweenTexts(original, optimized);
        if (sim == null) {
            log.warn("query semantic gate: embedding failed, fallback to original. originalLen={}, optimizedLen={}",
                    original.length(), optimized.length());
            return new QuerySemanticGateResponse(null, original, false);
        }

        boolean useOptimized = sim >= similarityThreshold;
        String finalQuery = useOptimized ? optimized : original;
        log.info("query semantic gate: similarity={}, threshold={}, usedOptimized={}", sim, similarityThreshold, useOptimized);
        return new QuerySemanticGateResponse(sim, finalQuery, useOptimized);
    }
}
