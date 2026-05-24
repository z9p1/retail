package com.retail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.entity.Product;
import com.retail.entity.RagChunk;
import com.retail.mapper.ProductMapper;
import com.retail.mapper.RagChunkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG：从商品等构建知识库，按向量相似度检索，供智能助手增强回答
 */
@Service
public class RagService {

    @Value("${rag.retrieve-top-k:5}")
    private int retrieveTopK = 5;

    @Autowired
    private RagChunkMapper ragChunkMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private SemanticSimilarityService semanticSimilarityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从商品表同步到 RAG：按商品生成 chunk（名称+描述），算 embedding 后写入 rag_chunk
     * 先按 source_type+source_id 删除再插入，实现全量覆盖
     */
    public void ingestFromProducts() {
        List<RagChunk> existing = ragChunkMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagChunk>()
                        .eq(RagChunk::getSourceType, "product"));
        for (RagChunk c : existing) ragChunkMapper.deleteById(c.getId());

        List<Product> products = productMapper.selectList(null);
        for (Product p : products) {
            String content = "商品：" + (p.getName() != null ? p.getName() : "")
                    + (StringUtils.hasText(p.getDescription()) ? "；描述：" + p.getDescription() : "");
            if (!StringUtils.hasText(content.trim())) continue;
            float[] vec = embeddingService.embed(content);
            if (vec == null) continue;
            RagChunk chunk = new RagChunk();
            chunk.setContent(content);
            chunk.setEmbeddingJson(toJsonVec(vec));
            chunk.setSourceType("product");
            chunk.setSourceId(String.valueOf(p.getId()));
            chunk.setCreateTime(LocalDateTime.now());
            ragChunkMapper.insert(chunk);
        }
    }

    /**
     * 按用户问题检索最相关的 topK 条 chunk，返回拼接后的参考文本（无结果返回空字符串）
     */
    public String retrieve(String userQuery) {
        if (!StringUtils.hasText(userQuery)) return "";
        float[] queryVec = embeddingService.embed(userQuery);
        if (queryVec == null) return "";

        List<RagChunk> all = ragChunkMapper.selectList(null);
        if (all.isEmpty()) return "";

        List<AbstractMap.SimpleEntry<RagChunk, Double>> withScore = new ArrayList<>();
        for (RagChunk c : all) {
            float[] chunkVec = parseEmbeddingJson(c.getEmbeddingJson());
            if (chunkVec == null) continue;
            double sim = semanticSimilarityService.cosineSimilarity(queryVec, chunkVec);
            withScore.add(new AbstractMap.SimpleEntry<>(c, sim));
        }
        List<RagChunk> sorted = withScore.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(retrieveTopK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (sorted.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【以下为与问题相关的参考知识，请基于此回答】\n");
        for (RagChunk c : sorted) sb.append("- ").append(c.getContent()).append("\n");
        return sb.toString();
    }

    private String toJsonVec(float[] vec) {
        if (vec == null) return "[]";
        try {
            List<Float> list = new ArrayList<>(vec.length);
            for (float v : vec) list.add(v);
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private float[] parseEmbeddingJson(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            List<Double> list = objectMapper.readValue(json, new TypeReference<List<Double>>() {});
            if (list == null) return null;
            float[] out = new float[list.size()];
            for (int i = 0; i < list.size(); i++) out[i] = list.get(i).floatValue();
            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
