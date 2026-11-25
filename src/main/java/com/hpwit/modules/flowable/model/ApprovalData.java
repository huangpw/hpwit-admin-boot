package com.hpwit.modules.flowable.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.DecimalNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApprovalData {

    private ApprovalData() {
    }

    public enum Category { LEAVE, EXPENSE, PURCHASE, SEAL, OTHER }

    public static final String DEMO_ORDER = "DEMO_ORDER";
    public static final String DEMO_ORDER_NO = "SO-DEMO-001";

    public enum FieldType { TEXT, TEXTAREA, NUMBER, DATE, BOOLEAN }

    public enum Status { DRAFT, PENDING, APPROVED, REJECTED, RETURNED, WITHDRAWN, CANCELLED, TERMINATED }

    public enum ActionType {
        START, APPROVE, REJECT, WITHDRAW, CANCEL, TRANSFER, ADD_SIGN, SIGN_APPROVE, RETURN,
        COMMENT, ATTACH, REMIND, CC, FORCE_APPROVE, FORCE_REJECT, TERMINATE, ASSIGN, MOVE,
        SAVE_DRAFT, DELETE, RESUBMIT, RESTART
    }

    public enum Operation {
        APPROVE, REJECT, WITHDRAW, CANCEL, TRANSFER, ADD_SIGN, RETURN, COMMENT, REMIND, CC,
        FORCE_APPROVE, FORCE_REJECT, TERMINATE, ASSIGN, MOVE, DELETE, RESTART,
        EDIT, RESUBMIT, ATTACH, MARK_READ
    }

    public enum NoticeKind { CC, REMIND }

    public record Command(@NotNull Operation operation, @Size(max = 64) String taskId,
                          @Pattern(regexp = "[1-9][0-9]{0,18}") String userId,
                          @Size(max = 80) String nodeId, @Size(max = 1000) String comment) {
    }

    public record BusinessPage(String code, String name, String component, String routePath, boolean enabled, int sortOrder) {
    }

    public record SaveBusinessPage(@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}") String code,
                                   @NotBlank @Size(max = 100) String name,
                                   @NotBlank @Size(max = 180) @Pattern(regexp = "[A-Za-z0-9_/-]+/approval\\.vue") String component,
                                   @Size(max = 180) @Pattern(regexp = "(?:/[A-Za-z0-9_-]+)+|") String routePath,
                                   boolean enabled, int sortOrder) {
    }

    public record InboxFilter(@Size(max = 150) String keyword, Category category, Status status,
                              Instant from, Instant until, Boolean unread) {
        public static final InboxFilter EMPTY = new InboxFilter(null, null, null, null, null, null);
    }

    public record InboxMetrics(long total, Map<Status, Long> statuses, long unread, long waitingDay) {
    }

    public record Overview(Map<Inbox, InboxMetrics> inboxes) {
    }

    public record TaskRef(@NotBlank @Size(max = 64) String instanceId, @NotBlank @Size(max = 64) String taskId) {
    }

    public record BatchCommand(@NotNull Operation operation, @NotEmpty @Size(max = 20) List<@NotNull @Valid TaskRef> items,
                               @Pattern(regexp = "[1-9][0-9]{0,18}") String userId, @Size(max = 1000) String comment) {
    }

    public record ReadNotices(@Size(max = 100) List<@NotBlank @Size(max = 64) String> instanceIds) {
    }

    public record Attachment(String id, String name, long size) {
    }

    public record AttachmentData(String name, String base64) {
    }

    public enum Inbox { MINE, TODO, DONE, CC, REMIND, ALL, ENDED }

    public record Field(
            @NotBlank @Pattern(regexp = "[a-z][A-Za-z0-9_]{0,39}") String key,
            @NotBlank @Size(max = 60) String label,
            @NotNull FieldType type,
            boolean required) {
    }

    public record SaveModel(
            @NotBlank @Size(max = 100) String name,
            @NotNull Category category,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 524288) String xml,
            @NotNull @Size(max = 24) List<@NotNull @Valid Field> fields,
            @Positive Integer revision,
            @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}") String businessPage,
            @Valid StatusCallback statusCallback) {
        public SaveModel(String name, Category category, String description, String xml, List<Field> fields,
                         Integer revision, String businessPage) {
            this(name, category, description, xml, fields, revision, businessPage, null);
        }
    }

    public record StatusCallback(@Size(max = 7) Map<@NotNull Status, @NotNull Integer> statusMapping,
                                 Map<@NotNull ActionType, @NotBlank String> operationEndpoints) {
        public StatusCallback {
            statusMapping = statusMapping == null ? Map.of() : statusMapping;
            operationEndpoints = operationEndpoints == null ? Map.of() : operationEndpoints;
        }
        @JsonCreator
        public static StatusCallback read(@JsonProperty("statusMapping") Map<Status, Integer> statusMapping,
                                          @JsonProperty("operationEndpoints") Map<ActionType, String> operationEndpoints,
                                          @JsonProperty("endpoint") String legacyStatusEndpoint) {
            // Mappings attached to retired HTTP endpoints must never become local database mappings.
            if (legacyStatusEndpoint != null) {
                return operationEndpoints == null || operationEndpoints.isEmpty()
                        ? null : new StatusCallback(Map.of(), operationEndpoints);
            }
            return new StatusCallback(statusMapping, operationEndpoints);
        }
    }

    public record ModelSummary(String id, String processKey, String name, Category category, String description,
                               int revision, Integer publishedRevision, String definitionId, Instant updatedAt,
                               Integer publishedVersion) {
        public ModelSummary withPublishedVersion(Integer version) {
            return new ModelSummary(id, processKey, name, category, description, revision, publishedRevision,
                    definitionId, updatedAt, version);
        }
    }

    public record Draft(ModelSummary model, String xml, List<Field> fields, String businessPage,
                        BusinessPage restoredPageConfig, StatusCallback statusCallback) {
    }

    public record Definition(String id, String modelId, String processKey, String name, Category category,
                             String description, int version, List<Field> fields, String businessPage, BusinessPage pageConfig,
                             StatusCallback statusCallback) {
    }

    public record ModelVersion(int version, String name, Instant publishedAt, boolean current) {
    }

    public record ModelVersions(int draftRevision, boolean unpublishedChanges, Page<ModelVersion> versions) {
    }

    public record ModelVersionDetail(Definition definition, String xml) {
    }

    public record RestoreModelVersion(@NotNull @Positive Integer revision) {
    }

    public record Order(
            @NotBlank @Size(max = 100) String customer,
            @NotBlank @Size(max = 100) String department,
            @NotBlank @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") String date,
            @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") String deliveryDate,
            @NotNull @Size(max = 50) String contact,
            @NotNull @Size(max = 30) String phone,
            @NotNull @Size(max = 250) String address,
            @NotNull @Size(max = 500) String note,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid OrderItem> items) {
        public BigDecimal amount() {
            return items.stream().map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(new BigDecimal("0.00"), BigDecimal::add);
        }
    }

    public record OrderApproval(String instanceId, Status status) {
    }

    public record OrderItem(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 120) String name,
            @Min(1) @Max(9999) int quantity,
            @NotNull @Digits(integer = 6, fraction = 2) @PositiveOrZero BigDecimal price) {
    }

    public record Start(
            @NotBlank @Size(max = 64) String definitionId,
            @NotBlank @Pattern(regexp = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}") String requestId,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 100) String businessNo,
            @NotNull @Size(max = 24) @JsonDeserialize(contentUsing = FormValueDeserializer.class)
            Map<String, JsonNode> values) {
    }

    public static class FormValueDeserializer extends JsonDeserializer<JsonNode> {
        @Override
        public JsonNode deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return DecimalNode.valueOf(parser.getDecimalValue());
            }
            return context.readTree(parser);
        }
    }

    public record Comment(@Size(max = 1000) String comment) {
    }

    public record Instance(String id, String definitionId, int definitionVersion, String definitionName,
                           Category category, String title, String businessNo, String applicantId,
                           String applicantName, Status status, Instant startedAt, Instant finishedAt,
                           String taskId, String taskName, Integer unread, List<Operation> listOperations,
                           Instant inboxAt, String activityComment, ActionType activityAction) {
        public Instance withTask(String taskId, String taskName) {
            return new Instance(id, definitionId, definitionVersion, definitionName, category, title, businessNo,
                    applicantId, applicantName, status, startedAt, finishedAt, taskId, taskName, unread, listOperations, inboxAt, activityComment, activityAction);
        }
        public Instance withListOperations(List<Operation> operations) {
            return new Instance(id, definitionId, definitionVersion, definitionName, category, title, businessNo,
                    applicantId, applicantName, status, startedAt, finishedAt, taskId, taskName, unread, operations, inboxAt, activityComment, activityAction);
        }
    }

    public record Action(String taskName, String actorId, String actorName, ActionType action,
                         String comment, Instant createdAt) {
    }

    public record Activity(String id, String name, String type, boolean active, boolean cancelled, Instant startedAt, Instant endedAt) {
    }

    public record Detail(Instance instance, List<Field> fields, Map<String, JsonNode> values, String xml,
                         List<Activity> activities, List<Action> actions, boolean canApprove, boolean canWithdraw,
                         String businessPage, BusinessPage pageConfig, List<Operation> operations,
                         List<Attachment> attachments, List<Option> moveTargets, String sourceId, Order order,
                         CallbackDelivery callbackDelivery) {
    }

    public record CallbackDelivery(String state, int attempts, Instant nextAttemptAt, Instant deliveredAt) {
    }

    public record Option(String value, String label) {
    }

    public record Page<T>(List<T> list, long total) {
    }
}
