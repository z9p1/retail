package com.retail.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 调用 OpenAI 兼容的 /embeddings 接口获取文本向量
 */
@Service
public class EmbeddingService {

    @Value("${agent.api-url:}")
    private String apiUrl;
    @Value("${agent.embedding-model:text-embedding-ada-002}")
    private String embeddingModel;

    @Autowired
    private AgentConfigService agentConfigService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取单条文本的 embedding 向量，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (!StringUtils.hasText(text)) return null;
        String key = agentConfigService.getAgentApiKey();
        if (!StringUtils.hasText(apiUrl) || !StringUtils.hasText(key)) return null;
        String url = apiUrl.endsWith("/") ? apiUrl + "embeddings" : apiUrl + "/embeddings";
        Map<String, Object> body = new HashMap<>();
        body.put("model", embeddingModel);
        body.put("input", text);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + key);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> respBody = resp.getBody();
            if (respBody == null) return null;
            List<Map<String, Object>> data = (List<Map<String, Object>>) respBody.get("data");
            if (data == null || data.isEmpty()) return null;
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            if (embedding == null) return null;
            float[] out = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) out[i] = embedding.get(i).floatValue();
            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
