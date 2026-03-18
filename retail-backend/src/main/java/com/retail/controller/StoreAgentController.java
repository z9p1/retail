package com.retail.controller;

import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.dto.StoreAgentChatResult;
import com.retail.exception.BusinessException;
import com.retail.service.AgentMessageService;
import com.retail.service.StoreAgentService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 店家侧智能助手：自然语言问工作台/流量数据。文本最多 20 字，30 秒限一次。
 * Dify 模式下会话与消息由 Java 持久化，响应带 conversation_id 供前端续聊。
 */
@RestController
@RequestMapping("/api/store/agent")
public class StoreAgentController {

    @Autowired
    private StoreAgentService storeAgentService;
    @Autowired
    private AgentMessageService agentMessageService;

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        String message = body != null ? body.get("message") : null;
        if (message == null || message.trim().isEmpty()) {
            return Result.fail(ResultCode.BAD_REQUEST, "请输入问题");
        }
        String trimmed = message.trim();
        if (trimmed.length() > 20) {
            return Result.fail(ResultCode.BAD_REQUEST, "问题最多20个字");
        }
        String conversationId = body != null ? body.get("conversation_id") : null;
        try {
            StoreAgentChatResult result = storeAgentService.chat(userId, trimmed, conversationId);
            Map<String, String> data = new HashMap<>();
            data.put("reply", result.getReply());
            if (result.getConversationId() != null) {
                data.put("conversation_id", result.getConversationId());
            }
            return Result.ok(data);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /** 流式输出：返回 SSE，前端边收边展示；仅 Dify 模式生效 */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) {
            SseEmitter e = new SseEmitter(0L);
            e.completeWithError(new IllegalStateException("请先登录"));
            return e;
        }
        String message = body != null ? body.get("message") : null;
        if (message == null || message.trim().isEmpty()) {
            SseEmitter e = new SseEmitter(0L);
            e.completeWithError(new IllegalArgumentException("请输入问题"));
            return e;
        }
        String trimmed = message.trim();
        if (trimmed.length() > 20) {
            SseEmitter e = new SseEmitter(0L);
            e.completeWithError(new IllegalArgumentException("问题最多20个字"));
            return e;
        }
        String conversationId = body != null ? body.get("conversation_id") : null;
        SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(120));
        CompletableFuture.runAsync(() -> {
            try {
                storeAgentService.chatStream(userId, trimmed, conversationId,
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event().data(chunk, MediaType.TEXT_PLAIN));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    },
                    (fullReply, ourConvId) -> {
                        try {
                            if (ourConvId != null) {
                                agentMessageService.add(ourConvId, "assistant", fullReply);
                                emitter.send(SseEmitter.event().name("done").data(ourConvId, MediaType.TEXT_PLAIN));
                            }
                            emitter.complete();
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data(e.getMessage() != null ? e.getMessage() : "请求失败", MediaType.TEXT_PLAIN));
                } catch (IOException ignored) { }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
