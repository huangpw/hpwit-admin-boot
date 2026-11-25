package com.hpwit.modules.flowable.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hpwit.modules.flowable.config.ApprovalCallbackProperties;
import com.hpwit.modules.flowable.model.ApprovalData.CallbackDelivery;
import com.hpwit.modules.flowable.model.ApprovalData.ActionType;
import com.hpwit.modules.flowable.model.ApprovalData.Option;
import com.hpwit.modules.flowable.model.ApprovalData.Status;
import com.hpwit.modules.flowable.model.ApprovalData.StatusCallback;
import com.hpwit.modules.flowable.service.BusinessApprovalStatusHandler.ApprovalReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.hpwit.modules.flowable.service.ApprovalBpmn.require;

@Service
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
public class ApprovalCallbacks {
    private static final Set<Status> MAPPED_STATES = Set.of(Status.PENDING, Status.APPROVED, Status.REJECTED,
            Status.RETURNED, Status.WITHDRAWN, Status.CANCELLED, Status.TERMINATED);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CLAIM_DURATION = Duration.ofMinutes(1);

    private final ApprovalCallbackProperties config;
    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final Set<String> mappedBusinessPages;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(REQUEST_TIMEOUT).build();

    public ApprovalCallbacks(ApprovalCallbackProperties config, JdbcClient jdbc, ObjectMapper json,
                             List<BusinessApprovalStatusHandler> handlers) {
        this.config = config;
        this.jdbc = jdbc;
        this.json = json;
        this.mappedBusinessPages = handlers.stream().filter(BusinessApprovalStatusHandler::supportsStatusMapping)
                .map(BusinessApprovalStatusHandler::businessPage).collect(Collectors.toUnmodifiableSet());
    }

    public List<Option> options(String businessPage) {
        var options = new ArrayList<Option>();
        if (supportsLocalMapping(businessPage)) {
            options.add(new Option("@local", "同库事务回写"));
        }
        config.endpoints().entrySet().stream()
                .filter(entry -> entry.getValue().businessPage().equals(businessPage))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> new Option(entry.getKey(),
                        entry.getValue().label() == null || entry.getValue().label().isBlank()
                                ? entry.getKey() : entry.getValue().label()))
                .forEachOrdered(options::add);
        return options;
    }

    private boolean supportsLocalMapping(String businessPage) {
        return businessPage != null && mappedBusinessPages.contains(businessPage);
    }

    void validate(String businessPage, StatusCallback callback) {
        if (callback == null) {
            return;
        }
        var mapping = callback.statusMapping();
        require(MAPPED_STATES.containsAll(mapping.keySet()) && mapping.values().stream().allMatch(Objects::nonNull),
                "请填写有效的审批状态映射");
        require(!mapping.isEmpty() || !callback.operationEndpoints().isEmpty(), "请配置状态映射或操作回写接口");
        if (!mapping.isEmpty()) {
            require(supportsLocalMapping(businessPage), "此业务页面未启用同库状态映射");
            require(mapping.keySet().equals(MAPPED_STATES), "同库回写须完整配置七项审批状态映射");
        }
        for (var entry : callback.operationEndpoints().entrySet()) {
            require(entry.getKey() != null, "请选择有效的审批操作");
            validateEndpoint(businessPage, entry.getValue());
        }
    }

    private void validateEndpoint(String businessPage, String key) {
        var endpoint = key == null ? null : config.endpoints().get(key);
        require(endpoint != null && endpoint.businessPage().equals(businessPage), "请选择与关联业务页面匹配的回写接口");
    }

    record OperationEvent(ActionType operation, String actorId, String taskId, String taskName,
                          String nodeId, String nextTaskId, String nextNodeId, String targetUserId, String sourceId) {
    }

    void enqueueOperation(String businessPage, ApprovalReference approval, Status status,
                          StatusCallback callback, OperationEvent event) {
        String endpoint = callback == null ? null : callback.operationEndpoints().get(event.operation());
        if (endpoint == null) {
            return;
        }
        String payload;
        try {
            payload = json.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("审批操作通知序列化失败", exception);
        }
        // The shared queue schema keeps business_status mandatory for historical status rows.
        jdbc.sql("""
                INSERT INTO flow_approval_status_callback (endpoint, business_page, business_no, instance_id,
                    applicant_id, approval_status, business_status, operation_id, operation_payload)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)
                """).params(endpoint, businessPage, approval.businessNo(), approval.instanceId(),
                        approval.applicantId(), status.name(), UUID.randomUUID().toString(), payload).update();
    }

    CallbackDelivery delivery(StatusCallback callback, String instanceId) {
        if (callback == null || callback.operationEndpoints().isEmpty()) {
            return null;
        }
        return jdbc.sql("""
                SELECT attempts, next_attempt_at, delivered_at FROM flow_approval_status_callback
                WHERE instance_id = ? AND operation_id <> ''
                ORDER BY CASE WHEN delivered_at IS NULL THEN 0 ELSE 1 END,
                    CASE WHEN delivered_at IS NULL THEN id END, id DESC LIMIT 1
                """).param(instanceId).query((rs, row) -> {
                    var delivered = rs.getTimestamp("delivered_at");
                    int attempts = rs.getInt("attempts");
                    return delivered != null ? new CallbackDelivery("DELIVERED", attempts, null, delivered.toInstant())
                            : new CallbackDelivery(attempts > 0 ? "RETRYING" : "PENDING",
                                    attempts, rs.getTimestamp("next_attempt_at").toInstant(), null);
                }).optional().orElse(null);
    }

    private record Delivery(long id, String endpoint, String businessPage, String businessNo, String businessId, String instanceId,
                            String applicantId, String approvalStatus, int attempts, String operationId,
                            String operationPayload) {
    }

    @Scheduled(fixedDelayString = "${hpwit.flowable.callbacks.poll-ms:5000}",
            initialDelayString = "${hpwit.flowable.callbacks.initial-delay-ms:5000}")
    void deliverPending() {
        if (config.endpoints().isEmpty()) {
            return;
        }
        var pending = jdbc.sql("""
                SELECT d.id, d.endpoint, d.business_page, d.business_no, d.instance_id, d.applicant_id,
                    d.approval_status, d.attempts, d.operation_id, d.operation_payload,
                    COALESCE(b.business_id, d.business_no) AS business_id
                FROM flow_approval_status_callback d
                LEFT JOIN flow_approval_business_binding b ON b.instance_id = d.instance_id
                WHERE d.operation_id <> '' AND d.delivered_at IS NULL AND d.next_attempt_at <= CURRENT_TIMESTAMP(3)
                  AND (d.lease_until IS NULL OR d.lease_until <= CURRENT_TIMESTAMP(3))
                  AND NOT EXISTS (
                    SELECT 1 FROM flow_approval_status_callback p
                    LEFT JOIN flow_approval_business_binding previous ON previous.instance_id = p.instance_id
                    WHERE p.business_page = d.business_page
                      AND (COALESCE(previous.business_id, p.business_no) = COALESCE(b.business_id, d.business_no)
                           OR (previous.instance_id IS NULL AND p.business_no = d.business_no))
                      AND p.operation_id <> '' AND p.delivered_at IS NULL AND p.id < d.id
                  )
                ORDER BY d.id LIMIT 20
                """).query((rs, row) -> new Delivery(rs.getLong("id"), rs.getString("endpoint"),
                        rs.getString("business_page"), rs.getString("business_no"), rs.getString("business_id"), rs.getString("instance_id"),
                        rs.getString("applicant_id"), rs.getString("approval_status"),
                        rs.getInt("attempts"), rs.getString("operation_id"), rs.getString("operation_payload"))).list();
        for (var delivery : pending) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            deliver(delivery);
        }
    }

    private void deliver(Delivery delivery) {
        String token = UUID.randomUUID().toString();
        int claimed = jdbc.sql("""
                UPDATE flow_approval_status_callback SET claim_token = ?, lease_until = ?
                WHERE id = ? AND delivered_at IS NULL AND next_attempt_at <= CURRENT_TIMESTAMP(3)
                  AND (lease_until IS NULL OR lease_until <= CURRENT_TIMESTAMP(3))
                """).params(token, Timestamp.from(Instant.now().plus(CLAIM_DURATION)), delivery.id()).update();
        if (claimed == 0) {
            return;
        }
        String error = send(delivery);
        if (error == null) {
            jdbc.sql("""
                    UPDATE flow_approval_status_callback SET delivered_at = CURRENT_TIMESTAMP(3), claim_token = NULL,
                        lease_until = NULL, last_error = NULL WHERE id = ? AND claim_token = ?
                    """).params(delivery.id(), token).update();
        } else {
            long retrySeconds = Math.min(3600, 5L << Math.min(delivery.attempts(), 10));
            jdbc.sql("""
                    UPDATE flow_approval_status_callback SET attempts = attempts + 1, next_attempt_at = ?,
                        claim_token = NULL, lease_until = NULL, last_error = ? WHERE id = ? AND claim_token = ?
                    """).params(Timestamp.from(Instant.now().plusSeconds(retrySeconds)), error, delivery.id(), token).update();
        }
    }

    private String send(Delivery delivery) {
        var endpoint = config.endpoints().get(delivery.endpoint());
        if (endpoint == null || !endpoint.businessPage().equals(delivery.businessPage())) {
            return "回写接口未登记或业务页面编码已变更";
        }
        try {
            String eventId = delivery.instanceId() + ":" + delivery.operationId();
            var body = json.<ObjectNode>valueToTree(Map.of("eventId", eventId, "sequence", Long.toString(delivery.id()),
                    "businessPage", delivery.businessPage(),
                    "businessNo", delivery.businessNo(), "businessId", delivery.businessId(), "instanceId", delivery.instanceId(),
                    "applicantId", delivery.applicantId(), "approvalStatus", delivery.approvalStatus(),
                    "eventType", "OPERATION"));
            body.setAll((ObjectNode) json.readTree(delivery.operationPayload()));
            var request = HttpRequest.newBuilder(endpoint.url()).timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json").header("Idempotency-Key", eventId)
                    .method(endpoint.method().name(), HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (endpoint.bearerToken() != null && !endpoint.bearerToken().isBlank()) {
                request.header("Authorization", "Bearer " + endpoint.bearerToken());
            }
            var response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            response.body().close();
            int code = response.statusCode();
            return code >= 200 && code < 300 ? null : "HTTP " + code;
        } catch (JsonProcessingException exception) {
            return "回写内容序列化失败";
        } catch (IOException | IllegalArgumentException exception) {
            return "回写请求失败: " + exception.getClass().getSimpleName();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "回写请求已中断";
        }
    }
}
