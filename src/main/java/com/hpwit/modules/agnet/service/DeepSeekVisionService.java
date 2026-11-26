package com.hpwit.modules.agnet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hpwit.common.result.ResultCode;
import com.hpwit.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 为识图功能统一校验图片、限制请求频率并调用 DeepSeek 视觉接口。
 * 图片和识别结果仅在本次请求中处理。
 */
@Service
@Slf4j
public class DeepSeekVisionService {

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final long MAX_PIXELS = 24_000_000;
    private final ObjectMapper objectMapper;
    private final WebClient deepSeek;
    private final String apiKey;
    private final String model;

    // 单实例最多同时处理 4 个请求。并发额度满时立即返回，由前端提示重试。
    private final Semaphore concurrentRequests = new Semaphore(4);

    // 以每个用户首次计数的时间为起点，保留一分钟的请求计数。
    private final Cache<Long, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    public DeepSeekVisionService(ObjectMapper objectMapper, WebClient.Builder webClient,
                                     @Value("${hpwit.agnet.deepseek.base-url}") String baseUrl,
                                     @Value("${hpwit.agnet.deepseek.api-key}") String apiKey,
                                     @Value("${hpwit.agnet.deepseek.model}") String model) {
        this.objectMapper = objectMapper;
        this.deepSeek = webClient.baseUrl(baseUrl).build();
        this.apiKey = apiKey.strip();
        this.model = model;
    }

    JsonNode recognize(long userId, MultipartFile file, String prompt, int maxTokens, Duration timeout) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BusinessException(ResultCode.INVALID_USER_INPUT, "请选择 8 MB 以内的图片");
        }
        if (apiKey.isEmpty()) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "识图服务尚未配置 DeepSeek API Key");
        }
        if (!concurrentRequests.tryAcquire()) {
            throw new BusinessException(ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED, "识图服务繁忙，请稍后重试");
        }
        try {
            // 在解码前计数，限制同一用户短时间内发起的识别尝试。
            if (requestCounts.get(userId, ignored -> new AtomicInteger()).incrementAndGet() > 8) {
                throw new BusinessException(ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED, "识别次数过多，请稍后重试");
            }
            return requestRecognition(readImage(file), prompt, maxTokens, timeout);
        } finally {
            concurrentRequests.release();
        }
    }

    /**
     * 根据文件内容确定 MIME 类型，校验像素数和解码结果，再将原始图片字节编码成 Data URL。
     */
    private String readImage(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                // ImageIO 从文件流选择解码器，WebP 解码器由 imageio-webp 依赖提供。
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw invalidImage();
                }
                ImageReader reader = readers.next();
                try {
                    String mimeType = switch (reader.getFormatName().toUpperCase(Locale.ROOT)) {
                        case "JPEG" -> "image/jpeg";
                        case "PNG" -> "image/png";
                        case "WEBP" -> "image/webp";
                        default -> throw invalidImage();
                    };
                    reader.setInput(input, true, true);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                        throw new BusinessException(ResultCode.INVALID_USER_INPUT, "图片分辨率过高，请缩小至 2400 万像素以内");
                    }
                    // 用降采样解码检查图片内容，减少校验时的像素缓冲。模型仍接收原图。
                    var decode = reader.getDefaultReadParam();
                    decode.setSourceSubsampling(4, 4, 0, 0);
                    reader.read(0, decode).flush();
                    return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            throw invalidImage();
        }
    }

    /**
     * 调用 DeepSeek Chat Completions 接口，在同一条消息中发送文本和图片。
     * 按 HTTP 状态区分鉴权、余额和限流等错误，给前端返回对应提示。
     */
    private JsonNode requestRecognition(String imageUrl, String prompt, int maxTokens, Duration timeout) {
        var body = Map.of(
                "model", model,
                "thinking", Map.of("type", "disabled"),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", "请按要求识别图片并输出 JSON。"),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                        ))
                )
        );
        JsonNode response;
        try {
            response = deepSeek.post()
                    .uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout)
                    .onErrorMap(TimeoutException.class, e -> upstreamError("图片识别超时，请重试"))
                    .block();
        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("DeepSeek image recognition returned HTTP {}", status);
            throw switch (status) {
                case 401, 403 -> upstreamError("DeepSeek 鉴权失败，请检查服务端配置");
                case 402 -> upstreamError("DeepSeek 账户余额不足");
                case 429 -> new BusinessException(ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED, "DeepSeek 服务繁忙，请稍后重试");
                default -> upstreamError("DeepSeek 识图服务暂时不可用，请稍后重试");
            };
        } catch (WebClientException e) {
            log.warn("DeepSeek image recognition failed: {}", e.getClass().getSimpleName());
            throw upstreamError("无法连接 DeepSeek 识图服务，请稍后重试");
        } catch (DecodingException e) {
            throw invalidResult();
        }
        return readResult(response);
    }

    /**
     * 仅接受正常结束、完整且唯一的 JSON 对象。
     */
    private JsonNode readResult(JsonNode response) {
        if (response == null) {
            throw upstreamError("识别服务返回了空结果，请重试");
        }
        JsonNode choice = response.path("choices").path(0);
        String content = choice.path("message").path("content").asText("");
        if (!"stop".equals(choice.path("finish_reason").asText()) || content.isBlank()) {
            throw upstreamError("识别结果未完整返回，请裁剪图片后重试");
        }
        try {
            JsonNode result = objectMapper.readerFor(JsonNode.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(content);
            if (!result.isObject()) {
                throw invalidResult();
            }
            return result;
        } catch (JsonProcessingException e) {
            throw invalidResult();
        }
    }

    static BusinessException invalidResult() {
        return upstreamError("识别服务返回了无效结果，请重试");
    }

    private static BusinessException invalidImage() {
        return new BusinessException(ResultCode.INVALID_USER_INPUT, "请选择有效的 JPEG、PNG 或 WebP 图片");
    }

    private static BusinessException upstreamError(String message) {
        return new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, message);
    }
}
