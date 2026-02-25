package com.retail.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.ResultCode;
import com.retail.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 店家侧智能助手：API Key 从数据库读取，文本最多 20 字，30 秒限调用一次。
 */
@Service
public class StoreAgentService {

    private static final int MAX_MESSAGE_LENGTH = 20;
    private static final int RATE_LIMIT_SECONDS = 30;
    private static final String RATE_LIMIT_KEY_PREFIX = "agent:ratelimit:";

    @Value("${agent.api-url:}")
    private String apiUrl;
    @Value("${agent.model:gpt-4o-mini}")
    private String model;

    @Autowired
    private WorkbenchService workbenchService;
    @Autowired
    private TrafficService trafficService;
    @Autowired
    private AgentConfigService agentConfigService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是店家线上零售系统的工作台助手。你只能使用提供的工具查询工作台汇总、流量汇总，然后根据数据用简短自然语言回答店家。回答用中文，简洁友好。"
            + "当用户询问库存、销量、订单、待发货、低库存、零库存、今日/本周数据等时，你必须先调用 get_workbench_summary 获取真实数据，再根据返回的数据回答；不要在不调用工具的情况下说“没有库存相关信息”或“请查看库存系统”。";

    @SuppressWarnings("unchecked")
    public String chat(Long userId, String userMessage) {
        if (userMessage == null || userMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "问题最多" + MAX_MESSAGE_LENGTH + "个字");
        }
        String rateKey = RATE_LIMIT_KEY_PREFIX + userId;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(set)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "请稍后再试，30秒内仅可调用一次");
        }
        String apiKey = agentConfigService.getAgentApiKey();
        if (!StringUtils.hasText(apiUrl) || !StringUtils.hasText(apiKey)) {
            return "智能助手尚未配置。请在数据库 agent_config 表配置 agent_api_key，并配置 agent.api-url 后使用。";
        }
        String url = apiUrl.endsWith("/") ? apiUrl + "chat/completions" : apiUrl + "/chat/completions";

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(mapOf("role", "system", "content", SYSTEM_PROMPT));
        messages.add(mapOf("role", "user", "content", userMessage));

        List<Map<String, Object>> tools = listOf(
                mapOf("type", "function", "function", mapOf(
                        "name", "get_workbench_summary",
                        "description", "获取工作台汇总：今日/本周订单数、销售额、待发货数、低库存数、零库存商品列表",
                        "parameters", mapOf("type", "object", "properties", Collections.emptyMap())
                )),
                mapOf("type", "function", "function", mapOf(
                        "name", "get_traffic_summary",
                        "description", "获取流量与销售汇总。参数 range：today=今日，7=最近7天，30=最近30天",
                        "parameters", mapOf(
                                "type", "object",
                                "properties", mapOf("range", mapOf("type", "string", "description", "today|7|30")),
                                "required", Collections.singletonList("range")
                        )
                ))
        );

        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("tools", tools);
        request.put("tool_choice", "auto");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null) return "助手暂无响应，请稍后再试。";

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return "助手暂无响应，请稍后再试。";
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            if (msg == null) return "助手暂无响应，请稍后再试。";

            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) msg.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                messages.add(msg);
                for (Map<String, Object> tc : toolCalls) {
                    String id = (String) tc.get("id");
                    Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                    String name = (String) fn.get("name");
                    String argsJson = (String) fn.get("arguments");
                    String toolResult = runTool(name, argsJson);
                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", id);
                    toolMsg.put("content", toolResult);
                    messages.add(toolMsg);
                }
                request.put("messages", messages);
                request.remove("tools");
                request.remove("tool_choice");
                entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);
                resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
                body = resp.getBody();
                if (body == null) return "助手处理数据后暂无响应。";
                choices = (List<Map<String, Object>>) body.get("choices");
                if (choices == null || choices.isEmpty()) return "助手处理数据后暂无响应。";
                msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg == null) return "助手处理数据后暂无响应。";
            }

            String content = (String) msg.get("content");
            return StringUtils.hasText(content) ? content.trim() : "暂无回复内容。";
        } catch (Exception e) {
            return "助手调用失败：" + (e.getMessage() != null ? e.getMessage() : "请检查网络与配置。");
        }
    }

    private String runTool(String name, String argsJson) {
        try {
            if ("get_workbench_summary".equals(name)) {
                Map<String, Object> data = workbenchService.getSummary();
                return objectMapper.writeValueAsString(data);
            }
            if ("get_traffic_summary".equals(name)) {
                String range = "7";
                if (StringUtils.hasText(argsJson)) {
                    Map<?, ?> args = objectMapper.readValue(argsJson, Map.class);
                    if (args != null && args.get("range") != null) range = args.get("range").toString();
                }
                Map<String, Object> data = trafficService.getTraffic(range);
                return objectMapper.writeValueAsString(data);
            }
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
        return "{\"error\":\"unknown tool\"}";
    }

    private static Map<String, Object> mapOf(Object... kvs) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return m;
    }

    @SafeVarargs
    private static List<Map<String, Object>> listOf(Map<String, Object>... items) {
        return Arrays.asList(items);
    }
}
