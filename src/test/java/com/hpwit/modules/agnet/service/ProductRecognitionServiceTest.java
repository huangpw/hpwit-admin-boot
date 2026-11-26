package com.hpwit.modules.agnet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpwit.common.result.ResultCode;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.modules.agnet.model.ProductRecognitionDraft;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRecognitionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendsImageToDeepSeekAndMapsOnlySupportedFields() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<JsonNode> request = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> modelContent = new AtomicReference<>("""
                {"productName":" 无线耳机 ","model":null,"categorySuggestion":"耳机音响",
                "tags":["白色","白色",7],"accessories":["充电盒"],"officialPrice":1899}
                """);
        server.createContext("/chat/completions", exchange -> {
            try (exchange) {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                request.set(mapper.readTree(exchange.getRequestBody()));
                byte[] response = mapper.writeValueAsBytes(Map.of("choices", List.of(Map.of(
                        "finish_reason", "stop", "message", Map.of("content", modelContent.get())))));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
        try {
            ProductRecognitionService service = service("http://127.0.0.1:" + server.getAddress().getPort());
            MockMultipartFile image = png();
            assertThat(service.recognize(1L, image)).isEqualTo(new ProductRecognitionDraft(
                    "无线耳机", null, null, "耳机音响", List.of("白色"), List.of("充电盒"), null));
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(request.get().at("/model").asText()).isEqualTo("deepseek-flash");
            assertThat(request.get().at("/response_format/type").asText()).isEqualTo("json_object");
            assertThat(request.get().at("/messages/1/content/1/image_url/url").asText())
                    .startsWith("data:image/png;base64,");

            // A 4×4 image encoded with cwebp's lossless mode.
            String webpBase64 = "UklGRhwAAABXRUJQVlA4TA8AAAAvA8AAAAcQ/Y/+ByKi/wEA";
            MockMultipartFile webp = new MockMultipartFile("file", "photo.webp", "image/webp",
                    Base64.getDecoder().decode(webpBase64));
            assertThat(service.recognize(1L, webp).productName()).isEqualTo("无线耳机");
            assertThat(request.get().at("/messages/1/content/1/image_url/url").asText())
                    .isEqualTo("data:image/webp;base64," + webpBase64);

            assertThat(service.recognizeMultiple(1L, List.of(image, webp)).productName()).isEqualTo("无线耳机");
            JsonNode content = request.get().at("/messages/1/content");
            assertThat(content.size()).isEqualTo(3);
            assertThat(content.at("/1/image_url/url").asText()).startsWith("data:image/png;base64,");
            assertThat(content.at("/2/image_url/url").asText()).isEqualTo("data:image/webp;base64," + webpBase64);

            modelContent.set("{\"productName\":null}");
            assertThatThrownBy(() -> service.recognize(1L, image))
                    .isInstanceOfSatisfying(BusinessException.class,
                            error -> assertThat(error.getResultCode()).isEqualTo(ResultCode.INVALID_USER_INPUT));

            modelContent.set("{\"productName\":\"耳机\"} extra");
            assertThatThrownBy(() -> service.recognize(1L, image))
                    .isInstanceOfSatisfying(BusinessException.class,
                            error -> assertThat(error.getResultCode()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsFileDisguisedAsImageBeforeCallingDeepSeek() {
        MockMultipartFile invalidFile = new MockMultipartFile("file", "photo.webp", "image/webp", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service("http://127.0.0.1:1").recognize(1L, invalidFile))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getResultCode()).isEqualTo(ResultCode.INVALID_USER_INPUT));
    }

    @Test
    void rejectsInvalidMultiImageCountOrTotalSizeBeforeCallingDeepSeek() {
        ProductRecognitionService service = service("http://127.0.0.1:1");
        MockMultipartFile file = new MockMultipartFile("files", "photo.png", "image/png", new byte[8 * 1024 * 1024]);
        assertThatThrownBy(() -> service.recognizeMultiple(1L, List.of(file)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("2 至 5 张"));
        assertThatThrownBy(() -> service.recognizeMultiple(1L, List.of(file, file, file, file)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("24 MB"));
    }

    private ProductRecognitionService service(String baseUrl) {
        return new ProductRecognitionService(new DeepSeekVisionService(mapper, WebClient.builder(), baseUrl, "test-key", "deepseek-flash"));
    }

    private static MockMultipartFile png() throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "photo.png", "image/png", output.toByteArray());
    }
}
