package com.hpwit.modules.agnet.service;

import com.hpwit.common.result.ResultCode;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.modules.agnet.model.AiChatData.Message;
import com.hpwit.modules.agnet.model.AiChatData.RenameSession;
import com.hpwit.modules.agnet.model.AiChatData.SendMessage;
import com.hpwit.modules.agnet.model.AiChatData.Session;
import com.hpwit.modules.agnet.model.AiChatData.Turn;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final Cache<Long, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    private final AiChatStore store;
    private final DeepSeekChatService deepSeek;

    public List<Session> sessions(long userId) {
        return store.sessions(userId);
    }

    public Session create(long userId) {
        return store.create(userId);
    }

    public List<Message> messages(long userId, String sessionId) {
        requireSession(userId, sessionId);
        return store.messages(userId, sessionId);
    }

    public Session rename(long userId, String sessionId, RenameSession form) {
        requireSession(userId, sessionId);
        if (!store.rename(userId, sessionId, form.title())) {
            throw new AiChatNotFoundException();
        }
        return requireSession(userId, sessionId);
    }

    public void delete(long userId, String sessionId) {
        requireSession(userId, sessionId);
        store.delete(userId, sessionId);
    }

    public Flux<String> send(long userId, String sessionId, SendMessage form) {
        requireSession(userId, sessionId);
        if (requestCounts.get(userId, ignored -> new AtomicInteger()).incrementAndGet() > 12) {
            throw new IllegalArgumentException("发送过于频繁，请稍后重试");
        }
        String content = form.content().strip();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("请输入消息内容");
        }
        store.addUserMessage(userId, sessionId, content);
        StringBuilder answer = new StringBuilder();
        List<Turn> context = store.context(sessionId);
        return deepSeek.stream(context)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(answer::append)
                .doOnComplete(() -> {
                    if (!answer.isEmpty()) {
                        store.addAssistantMessage(userId, sessionId, answer.toString());
                    }
                });
    }

    private Session requireSession(long userId, String sessionId) {
        return store.session(userId, sessionId).orElseThrow(AiChatNotFoundException::new);
    }

    public static final class AiChatNotFoundException extends BusinessException {
        public AiChatNotFoundException() {
            super(ResultCode.INVALID_USER_INPUT, "找不到该会话");
        }
    }
}
