package com.retail.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 两段文本的语义相似度：同一 embedding 模型下各算向量后做余弦相似度。
 */
@Service
public class SemanticSimilarityService {

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 余弦相似度，范围约 [-1, 1]；长度不一致或空向量返回 0。
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na <= 0 || nb <= 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 对两段文本分别 embedding 后计算余弦相似度；任一侧 embedding 失败返回 null。
     */
    public Double similarityBetweenTexts(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) return null;
        float[] va = embeddingService.embed(a.trim());
        float[] vb = embeddingService.embed(b.trim());
        if (va == null || vb == null) return null;
        return cosineSimilarity(va, vb);
    }
}
