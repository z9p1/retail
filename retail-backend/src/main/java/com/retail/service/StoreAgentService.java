package com.retail.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.ResultCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.retail.dto.StoreAgentChatResult;
import com.retail.entity.AgentConversation;
import com.retail.entity.AgentErrorLog;
import com.retail.entity.AgentMessage;
import com.retail.entity.Product;
import com.retail.exception.BusinessException;
import com.retail.mapper.AgentErrorLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 店家侧智能助手：API Key 从数据库读取，文本最多 20 字，30 秒限调用一次。
 * Dify 模式下会话与消息由 Java 持久化（agent_conversation / agent_message），支持多轮续聊与跨设备。
 */
@Service
public class StoreAgentService {

    private static final int MAX_MESSAGE_LENGTH = 20;
    private static final int RATE_LIMIT_SECONDS = 30;
    private static final String RATE_LIMIT_KEY_PREFIX = "agent:ratelimit:";
    /** 调用失败时给用户的固定话术，不暴露内部错误；真实错误进 agent_error_log + 日志 */
    private static final String FAILED_REPLY_FIXED = "服务暂时不可用，请稍后再试。";

    private static final Logger log = LoggerFactory.getLogger(StoreAgentService.class);

    @Value("${agent.api-url:}")
    private String apiUrl;
    @Value("${agent.model:gpt-4o-mini}")
    private String model;
    @Value("${agent.provider:openai}")
    private String provider;
    @Value("${agent.dify-timeout-seconds:60}")
    private int difyTimeoutSeconds;
    @Value("${agent.dify-stream-timeout-seconds:120}")
    private int difyStreamTimeoutSeconds;
    @Value("${agent.conversation-max-messages:20}")
    private int conversationMaxMessages;

    @Autowired
    private WorkbenchService workbenchService;
    @Autowired
    private TrafficService trafficService;
    @Autowired
    private AgentConfigService agentConfigService;
    @Autowired
    private RagService ragService;
    @Autowired
    private ProductService productService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AgentConversationService agentConversationService;
    @Autowired
    private AgentMessageService agentMessageService;
    @Autowired
    private AgentErrorLogMapper agentErrorLogMapper;

    private RestTemplate restTemplate;
    private RestTemplate restTemplateStream;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int ms = difyTimeoutSeconds * 1000;
        factory.setConnectTimeout(ms);
        factory.setReadTimeout(ms);
        this.restTemplate = new RestTemplate(factory);
        SimpleClientHttpRequestFactory factoryStream = new SimpleClientHttpRequestFactory();
        int streamMs = Math.max(difyStreamTimeoutSeconds, 60) * 1000;
        factoryStream.setConnectTimeout(streamMs);
        factoryStream.setReadTimeout(streamMs);
        this.restTemplateStream = new RestTemplate(factoryStream);
    }

    private static final String SYSTEM_PROMPT = "你是店家线上零售系统的工作台助手。你只能使用提供的工具查询工作台汇总、流量汇总，然后根据数据用简短自然语言回答店家。回答用中文，简洁友好。"
            + "当用户询问库存、销量、订单、待发货、低库存、零库存、今日/本周数据等时，你必须先调用 get_workbench_summary 获取真实数据，再根据返回的数据回答；不要在不调用工具的情况下说“没有库存相关信息”或“请查看库存系统”。";

    @SuppressWarnings("unchecked")
    public StoreAgentChatResult chat(Long userId, String userMessage, String conversationId) {
        if (userMessage == null || userMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "问题最多" + MAX_MESSAGE_LENGTH + "个字");
        }
        String rateKey = RATE_LIMIT_KEY_PREFIX + userId;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(set)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "请稍后再试，30秒内仅可调用一次");
        }
        if ("dify".equalsIgnoreCase(provider)) {
            String apiKey = agentConfigService.getDifyApiKey();
            if (!StringUtils.hasText(apiUrl) || !StringUtils.hasText(apiKey)) {
                return new StoreAgentChatResult("智能助手尚未配置。请在 agent_config 表配置 dify_app_current 与对应 dify_app_xxx 的 API Key，或配置 agent_api_key，并配置 agent.api-url。", null);
            }
            return chatWithDifyAndPersist(userId, userMessage, apiKey, conversationId);
        }

        String apiKey = agentConfigService.getAgentApiKey();
        if (!StringUtils.hasText(apiUrl) || !StringUtils.hasText(apiKey)) {
            return new StoreAgentChatResult("智能助手尚未配置。请在数据库 agent_config 表配置 agent_api_key，并配置 agent.api-url 后使用。", null);
        }

        String url = apiUrl.endsWith("/") ? apiUrl + "chat/completions" : apiUrl + "/chat/completions";

        String ragContext = ragService.retrieve(userMessage);
        String systemContent = SYSTEM_PROMPT;
        if (StringUtils.hasText(ragContext)) {
            systemContent = systemContent + "\n\n" + ragContext;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(mapOf("role", "system", "content", systemContent));
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
                )),
                mapOf("type", "function", "function", mapOf(
                        "name", "get_product_stock",
                        "description", "按商品名称模糊查询库存信息。适用于用户问“某个商品还有多少库存”时使用。",
                        "parameters", mapOf(
                                "type", "object",
                                "properties", mapOf("name", mapOf("type", "string", "description", "商品名称，如 Organic Apple")),
                                "required", Collections.singletonList("name")
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
            if (body == null) return new StoreAgentChatResult("助手暂无响应，请稍后再试。", null);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return new StoreAgentChatResult("助手暂无响应，请稍后再试。", null);
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            if (msg == null) return new StoreAgentChatResult("助手暂无响应，请稍后再试。", null);

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
                if (body == null) return new StoreAgentChatResult("助手处理数据后暂无响应。", null);
                choices = (List<Map<String, Object>>) body.get("choices");
                if (choices == null || choices.isEmpty()) return new StoreAgentChatResult("助手处理数据后暂无响应。", null);
                msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg == null) return new StoreAgentChatResult("助手处理数据后暂无响应。", null);
            }

            String content = (String) msg.get("content");
            String reply = StringUtils.hasText(content) ? content.trim() : "暂无回复内容。";
            return new StoreAgentChatResult(reply, null);
        } catch (Exception e) {
            return new StoreAgentChatResult("助手调用失败：" + (e.getMessage() != null ? e.getMessage() : "请检查网络与配置。"), null);
        }
    }

    /**
     * Dify 模式流式：边收 Dify SSE 边通过 onChunk 推送，结束时 onComplete(fullAnswer, ourConversationId)。
     * 限流、会话与 user 消息落库在此内完成；assistant 完整内容由调用方在 onComplete 后落库。
     */
    public void chatStream(Long userId, String userMessage, String conversationIdFromFront, Consumer<String> onChunk, BiConsumer<String, String> onComplete) {
        if (!"dify".equalsIgnoreCase(provider)) {
            onComplete.accept("当前仅 Dify 模式支持流式输出。", null);
            return;
        }
        if (userMessage == null || userMessage.length() > MAX_MESSAGE_LENGTH) {
            onComplete.accept("问题最多" + MAX_MESSAGE_LENGTH + "个字。", null);
            return;
        }
        String rateKey = RATE_LIMIT_KEY_PREFIX + userId;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(set)) {
            onComplete.accept("请稍后再试，30秒内仅可调用一次。", null);
            return;
        }
        String apiKey = agentConfigService.getDifyApiKey();
        if (!StringUtils.hasText(apiUrl) || !StringUtils.hasText(apiKey)) {
            onComplete.accept("智能助手尚未配置。请在 agent_config 表配置 dify_app_current 与对应 API Key，并配置 agent.api-url。", null);
            return;
        }
        AgentConversation conv = agentConversationService.getOrCreate(userId, conversationIdFromFront);
        agentMessageService.add(conv.getConversationId(), "user", userMessage);
        String queryForDify = buildQueryWithJavaContext(conv.getConversationId(), userMessage);
        String ourConvId = conv.getConversationId();
        try {
            doDifyStream(userId, queryForDify, userMessage, apiKey, ourConvId, onChunk, onComplete);
        } catch (Exception e) {
            log.error("Dify stream failed, conversationId={}, userId={}", ourConvId, userId, e);
            AgentErrorLog err = new AgentErrorLog();
            err.setConversationId(ourConvId);
            err.setUserId(userId);
            err.setErrorCode("DIFY_CALL_FAILED");
            err.setErrorMessage(e.getMessage() != null ? e.getMessage() : "unknown");
            err.setDetail(stackTraceString(e));
            err.setCreatedAt(LocalDateTime.now());
            try {
                agentErrorLogMapper.insert(err);
            } catch (Exception ex) {
                log.warn("Failed to insert agent_error_log", ex);
            }
            onComplete.accept(FAILED_REPLY_FIXED, ourConvId);
        }
    }

    @SuppressWarnings("unchecked")
    private void doDifyStream(Long userId, String query, String currentUserMessage, String apiKey, String ourConvId,
                              Consumer<String> onChunk, BiConsumer<String, String> onComplete) throws Exception {
        String baseUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        String url = baseUrl + "chat-messages";
        String ragContext = ragService.retrieve(currentUserMessage);
        String finalQuery = StringUtils.hasText(ragContext) ? "【参考知识】\n" + ragContext + "\n\n" + query : query;
        Map<String, Object> body = new HashMap<>();
        body.put("query", finalQuery);
        body.put("user", "store-" + userId);
        body.put("response_mode", "streaming");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("name", "店家助手");
        inputs.put("name2", "store-" + userId);
        body.put("inputs", inputs);
        String bodyJson = objectMapper.writeValueAsString(body);
        java.net.URI uri = new java.net.URI(url);
        RequestCallback requestCallback = (ClientHttpRequest request) -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().set("Authorization", "Bearer " + apiKey);
            request.getBody().write(bodyJson.getBytes(StandardCharsets.UTF_8));
        };
        ResponseExtractor<Void> responseExtractor = (ClientHttpResponse response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Dify returned " + response.getStatusCode());
            }
            StringBuilder fullAnswer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String json = line.substring(6).trim();
                        if ("[DONE]".equals(json) || json.isEmpty()) continue;
                        try {
                            Map<String, Object> ev = objectMapper.readValue(json, Map.class);
                            String event = ev != null ? (String) ev.get("event") : null;
                            Object answerObj = ev != null ? ev.get("answer") : null;
                            String answerDelta = answerObj != null ? answerObj.toString() : null;
                            if ("message".equals(event) && StringUtils.hasText(answerDelta)) {
                                fullAnswer.append(answerDelta);
                                onChunk.accept(answerDelta);
                            } else if ("message_end".equals(event) || "workflow_finished".equals(event)) {
                                break;
                            }
                        } catch (Exception ignored) { /* skip malformed line */ }
                    }
                }
            }
            String full = fullAnswer.length() > 0 ? fullAnswer.toString() : "助手暂无回复内容。";
            onComplete.accept(full, ourConvId);
            return null;
        };
        restTemplateStream.execute(uri, HttpMethod.POST, requestCallback, responseExtractor);
    }

    private StoreAgentChatResult chatWithDifyAndPersist(Long userId, String userMessage, String apiKey, String conversationIdFromFront) {
        AgentConversation conv = agentConversationService.getOrCreate(userId, conversationIdFromFront);
        agentMessageService.add(conv.getConversationId(), "user", userMessage);
        String queryForDify = buildQueryWithJavaContext(conv.getConversationId(), userMessage);
        try {
            DifyChatResult dr = chatWithDify(userId, queryForDify, userMessage, apiKey);
            agentMessageService.add(conv.getConversationId(), "assistant", dr.answer);
            return new StoreAgentChatResult(dr.answer, conv.getConversationId());
        } catch (Exception e) {
            log.error("Dify call failed, conversationId={}, userId={}", conv.getConversationId(), userId, e);
            AgentErrorLog err = new AgentErrorLog();
            err.setConversationId(conv.getConversationId());
            err.setUserId(userId);
            err.setErrorCode("DIFY_CALL_FAILED");
            err.setErrorMessage(e.getMessage() != null ? e.getMessage() : "unknown");
            err.setDetail(stackTraceString(e));
            err.setCreatedAt(LocalDateTime.now());
            try {
                agentErrorLogMapper.insert(err);
            } catch (Exception ex) {
                log.warn("Failed to insert agent_error_log", ex);
            }
            agentMessageService.add(conv.getConversationId(), "assistant", FAILED_REPLY_FIXED);
            return new StoreAgentChatResult(FAILED_REPLY_FIXED, conv.getConversationId());
        }
    }

    private static String stackTraceString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        return s.length() > 2000 ? s.substring(0, 2000) + "..." : s;
    }

    /**
     * 从 Java agent_message 表加载最近 N 条，拼成「历史 + 当前问题」作为发给 Dify 的 query，Dify 不再依赖自己的会话存储。
     */
    private String buildQueryWithJavaContext(String ourConversationId, String currentUserMessage) {
        int limit = conversationMaxMessages > 0 ? conversationMaxMessages : 20;
        List<AgentMessage> list = agentMessageService.listRecent(ourConversationId, limit);
        if (list.size() <= 1) {
            return currentUserMessage;
        }
        StringBuilder history = new StringBuilder();
        for (int i = 0; i < list.size() - 1; i++) {
            AgentMessage m = list.get(i);
            String role = "user".equals(m.getRole()) ? "用户" : "助手";
            history.append(role).append("：").append(m.getContent() != null ? m.getContent().trim() : "").append("\n");
        }
        return "【以下为历史对话】\n" + history + "【用户当前问题】\n" + currentUserMessage;
    }

    /**
     * 调用 Dify /v1/chat-messages。不传 conversation_id，上下文已由 Java 拼在 query 中。RAG 仅按当前问题检索。
     * 抛异常时由 chatWithDifyAndPersist 捕获并写 agent_error_log、落受控提示。
     */
    @SuppressWarnings("unchecked")
    private DifyChatResult chatWithDify(Long userId, String query, String currentUserMessage, String apiKey) throws Exception {
        String baseUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        String url = baseUrl + "chat-messages";
        String ragContext = ragService.retrieve(currentUserMessage);
        String finalQuery = query;
        if (StringUtils.hasText(ragContext)) {
            finalQuery = "【参考知识】\n" + ragContext + "\n\n" + query;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("query", finalQuery);
        body.put("user", "store-" + userId);
        body.put("response_mode", "blocking");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("name", "店家助手");
        inputs.put("name2", "store-" + userId);
        body.put("inputs", inputs);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> respBody = resp.getBody();
        if (respBody == null) return new DifyChatResult("助手暂无响应，请稍后再试。", null);
        String answer = (String) respBody.get("answer");
        String newDifyConvId = null;
        Object newConvId = respBody.get("conversation_id");
        if (newConvId != null && StringUtils.hasText(newConvId.toString())) {
            newDifyConvId = newConvId.toString();
        }
        return new DifyChatResult(StringUtils.hasText(answer) ? answer.trim() : "助手暂无回复内容。", newDifyConvId);
    }

    private static class DifyChatResult {
        final String answer;
        final String difyConversationId;
        DifyChatResult(String answer, String difyConversationId) {
            this.answer = answer;
            this.difyConversationId = difyConversationId;
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
            if ("get_product_stock".equals(name)) {
                String keyword = null;
                if (StringUtils.hasText(argsJson)) {
                    Map<?, ?> args = objectMapper.readValue(argsJson, Map.class);
                    if (args != null && args.get("name") != null) keyword = args.get("name").toString();
                }
                if (!StringUtils.hasText(keyword)) {
                    return "{\"error\":\"name 参数不能为空\"}";
                }
                IPage<Product> page = productService.listForStore(1, 10, keyword.trim(), null);
                Map<String, Object> resp = new HashMap<>();
                resp.put("query", keyword.trim());
                resp.put("total", page.getTotal());
                List<Map<String, Object>> items = new ArrayList<>();
                for (Product p : page.getRecords()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("stock", p.getStock());
                    m.put("status", p.getStatus());
                    items.add(m);
                }
                resp.put("items", items);
                return objectMapper.writeValueAsString(resp);
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
