package com.hpwit.modules.agnet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpwit.modules.agnet.model.AiChatData.Turn;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekChatServiceTest {

    @Test
    void streamsOnlyAssistantTextAndSendsRecentConversation() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            try (exchange) {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = ("data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"世界\"}}]}\n\n"
                        + "data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
        try {
            var service = new DeepSeekChatService(new ObjectMapper(), WebClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "deepseek-flash");
            List<String> chunks = service.stream(List.of(new Turn("user", "你好")))
                    .collectList().block(Duration.ofSeconds(5));

            assertThat(chunks).containsExactly("你好", "世界");
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(body.get()).contains("\"stream\":true", "\"role\":\"system\"", "\"content\":\"你好\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void failsClearlyWhenServerKeyIsNotConfigured() {
        var service = new DeepSeekChatService(new ObjectMapper(), WebClient.builder(),
                "http://127.0.0.1:1", " ", "deepseek-flash");

        assertThatThrownBy(() -> service.stream(List.of(new Turn("user", "你好"))).blockLast())
                .isInstanceOf(DeepSeekChatService.ChatException.class)
                .hasMessageContaining("尚未配置");
    }
}
