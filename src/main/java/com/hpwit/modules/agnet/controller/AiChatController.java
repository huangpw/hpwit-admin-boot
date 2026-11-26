package com.hpwit.modules.agnet.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.hpwit.common.result.Result;
import com.hpwit.modules.agnet.model.AiChatData.Message;
import com.hpwit.modules.agnet.model.AiChatData.RenameSession;
import com.hpwit.modules.agnet.model.AiChatData.SendMessage;
import com.hpwit.modules.agnet.model.AiChatData.Session;
import com.hpwit.modules.agnet.service.AiChatService;
import com.hpwit.modules.agnet.service.AiChatService.AiChatNotFoundException;
import com.hpwit.modules.agnet.service.DeepSeekChatService.ChatException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "AI 聊天助手")
@RestController
@RequestMapping("/v1/agnet/chat")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiChatService chat;

    @Operation(summary = "获取当前用户的聊天会话")
    @GetMapping("/sessions")
    public Result<List<Session>> sessions() {
        return Result.success(chat.sessions(userId()));
    }

    @Operation(summary = "创建聊天会话")
    @PostMapping("/sessions")
    public Result<Session> createSession() {
        return Result.success(chat.create(userId()));
    }

    @Operation(summary = "读取会话消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<Message>> messages(@PathVariable String sessionId) {
        return Result.success(chat.messages(userId(), sessionId));
    }

    @Operation(summary = "重命名聊天会话")
    @PatchMapping("/sessions/{sessionId}")
    public Result<Session> rename(@PathVariable String sessionId, @Valid @RequestBody RenameSession form) {
        return Result.success(chat.rename(userId(), sessionId, form));
    }

    @Operation(summary = "删除聊天会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> delete(@PathVariable String sessionId) {
        chat.delete(userId(), sessionId);
        return Result.success();
    }

    @Operation(summary = "发送消息并接收流式 AI 回复")
    @PostMapping(value = "/sessions/{sessionId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@PathVariable String sessionId, @Valid @RequestBody SendMessage form) {
        long userId = userId();
        SseEmitter emitter = new SseEmitter(150_000L);
        final var subscription = new java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable>();
        emitter.onCompletion(() -> dispose(subscription));
        emitter.onTimeout(() -> dispose(subscription));

        try {
            var stream = chat.send(userId, sessionId, form);
            subscription.set(stream.subscribe(
                    chunk -> sendEvent(emitter, "chunk", Map.of("content", chunk)),
                    error -> {
                        String message = error instanceof ChatException || error instanceof AiChatNotFoundException
                                ? error.getMessage() : "AI 回复失败，请稍后重试";
                        if (!(error instanceof ChatException || error instanceof AiChatNotFoundException)) {
                            log.warn("AI chat stream failed: {}", error.getClass().getSimpleName());
                        }
                        sendEvent(emitter, "error", Map.of("message", message));
                        emitter.complete();
                    },
                    () -> {
                        sendEvent(emitter, "done", Map.of());
                        emitter.complete();
                    }));
        } catch (RuntimeException error) {
            String message = error instanceof AiChatNotFoundException || error instanceof IllegalArgumentException
                    ? error.getMessage() : "无法开始 AI 回复，请稍后重试";
            sendEvent(emitter, "error", Map.of("message", message));
            emitter.complete();
        }
        return emitter;
    }

    private static long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    private static void dispose(java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable> subscription) {
        var current = subscription.get();
        if (current != null && !current.isDisposed()) {
            current.dispose();
        }
    }

    private static void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException error) {
            log.debug("AI chat client disconnected while sending {}", name);
            emitter.completeWithError(error);
        }
    }
}
