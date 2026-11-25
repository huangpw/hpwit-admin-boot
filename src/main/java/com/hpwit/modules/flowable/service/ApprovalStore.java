package com.hpwit.modules.flowable.service;

import com.hpwit.modules.flowable.model.ApprovalData.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static com.hpwit.common.constant.SysConstant.ROOT_ROLE_CODE;
import static com.hpwit.modules.flowable.service.ApprovalAccess.WORK;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
class ApprovalStore {

    record ModelRow(ModelSummary summary, String xml, String fields) {
    }

    record InstanceRow(Instance instance, String values) {
    }

    record BusinessBinding(String page, String businessId, boolean active) {
    }

    record ExistingRequest(String id, boolean draft) {
    }

    record User(long id, String name) {
        String engineId() {
            return Long.toString(id);
        }
    }

    private static final RowMapper<ModelSummary> MODEL = (rs, row) -> new ModelSummary(
            rs.getString("id"), rs.getString("process_key"), rs.getString("name"),
            Category.valueOf(rs.getString("category")), rs.getString("description"),
            rs.getInt("revision"), rs.getObject("published_revision", Integer.class),
            rs.getString("definition_id"), instant(rs, "updated_at"), null);
    private static final RowMapper<Instance> INSTANCE = (rs, row) -> new Instance(
            rs.getString("id"), rs.getString("definition_id"), rs.getInt("definition_version"),
            rs.getString("definition_name"), Category.valueOf(rs.getString("category")),
            rs.getString("title"), rs.getString("business_no"), rs.getString("applicant_id"),
            rs.getString("applicant_name"), ("RUNNING".equals(rs.getString("status")) ? Status.PENDING : Status.valueOf(rs.getString("status"))),
            instant(rs, "started_at"), instant(rs, "finished_at"), null, null, null, List.of(), null, null, null);
    private static final String MODEL_COLUMNS =
            "id, process_key, name, category, description, revision, published_revision, definition_id, updated_at";
    private static final String INSTANCE_COLUMNS =
            "id, definition_id, definition_version, definition_name, category, title, business_no, applicant_id, "
                    + "applicant_name, status, started_at, finished_at";
    private static final String USER_NAME = "COALESCE(NULLIF(u.nickname, ''), u.username, CAST(u.id AS CHAR))";
    private static final String ROOT_ROLE = ROOT_ROLE_CODE.toLowerCase(Locale.ROOT);
    private static final String ACTIVE_ROLES = """
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.is_deleted = 0
            WHERE ur.user_id = ?
            """;
    private static final String CAN_WORK = """
            AND EXISTS (
                SELECT 1 FROM sys_user_role wur
                JOIN sys_role wr ON wr.id = wur.role_id AND wr.status = 1 AND wr.is_deleted = 0
                WHERE wur.user_id = u.id AND (LOWER(wr.code) = ? OR EXISTS (
                    SELECT 1 FROM sys_role_menu rm JOIN sys_menu m ON m.id = rm.menu_id
                    WHERE rm.role_id = wr.id AND m.type = 4 AND m.perm = ?
                ))
            )
            """;
    private static final String APPROVAL_ROLES = """
            FROM sys_role r
            WHERE r.status = 1 AND r.is_deleted = 0 AND EXISTS (
                SELECT 1 FROM sys_user_role ur
                JOIN sys_user u ON u.id = ur.user_id AND u.status = 1 AND u.is_deleted = 0
                WHERE ur.role_id = r.id
            """ + CAN_WORK + ")";

    private final JdbcClient jdbc;

    Optional<String> order(long userId) {
        return jdbc.sql("SELECT order_json FROM flow_approval_order WHERE user_id = ?")
                .param(userId).query(String.class).optional();
    }

    Optional<OrderApproval> orderApproval(long userId) {
        return jdbc.sql("SELECT instance_id, status FROM flow_approval_order_status WHERE user_id = ?")
                .param(userId).query((rs, row) -> new OrderApproval(rs.getString("instance_id"),
                        Status.valueOf(rs.getString("status")))).optional();
    }

    void saveOrder(long userId, String order) {
        var update = jdbc.sql("UPDATE flow_approval_order SET order_json = ? WHERE user_id = ?").params(order, userId);
        if (update.update() != 0) {
            return;
        }
        try {
            jdbc.sql("INSERT INTO flow_approval_order (user_id, order_json) VALUES (?, ?)")
                    .params(userId, order).update();
        } catch (DuplicateKeyException concurrentInsert) {
            update.update();
        }
    }

    Page<ModelSummary> models(int offset, int size, String keyword) {
        Map<String, String> filters = text(keyword).isEmpty() ? Map.of()
                : Map.of("keyword", searchPattern(keyword).toLowerCase(Locale.ROOT));
        String where = filters.isEmpty() ? "" : """
                 WHERE LOWER(name) LIKE :keyword ESCAPE '!' OR LOWER(description) LIKE :keyword ESCAPE '!'
                """;
        long count = jdbc.sql("SELECT COUNT(*) FROM flow_approval_model" + where)
                .params(filters).query(Long.class).single();
        var list = jdbc.sql("SELECT " + MODEL_COLUMNS + " FROM flow_approval_model" + where
                        + " ORDER BY updated_at DESC, id LIMIT :size OFFSET :offset")
                .params(filters).param("size", size).param("offset", offset).query(MODEL).list();
        return new Page<>(list, count);
    }

    Optional<ModelRow> model(String id, boolean lock) {
        return jdbc.sql("SELECT " + MODEL_COLUMNS + ", bpmn_xml, fields_json FROM flow_approval_model WHERE id = ?"
                        + (lock ? " FOR UPDATE" : ""))
                .param(id)
                .query((rs, row) -> new ModelRow(MODEL.mapRow(rs, row), rs.getString("bpmn_xml"), rs.getString("fields_json")))
                .optional();
    }

    List<String> publishedDefinitionIds() {
        return jdbc.sql("""
                SELECT definition_id FROM flow_approval_model WHERE definition_id IS NOT NULL ORDER BY name, id
                """).query(String.class).list();
    }

    void insertModel(String id, String key, SaveModel form, String fields) {
        jdbc.sql("""
                INSERT INTO flow_approval_model (id, process_key, name, category, description, bpmn_xml, fields_json, revision)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """).params(id, key, form.name().strip(), form.category().name(), text(form.description()), form.xml(), fields)
                .update();
    }

    void updateModel(String id, SaveModel form, String fields) {
        jdbc.sql("""
                UPDATE flow_approval_model SET name = ?, category = ?, description = ?, bpmn_xml = ?, fields_json = ?,
                revision = revision + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?
                """).params(form.name().strip(), form.category().name(), text(form.description()), form.xml(), fields, id)
                .update();
    }

    void publishModel(String id, String definitionId) {
        jdbc.sql("""
                UPDATE flow_approval_model SET definition_id = ?, published_revision = revision,
                updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?
                """).params(definitionId, id).update();
    }

    Optional<User> user(long id) {
        return jdbc.sql("SELECT u.id, " + USER_NAME + " AS display_name FROM sys_user u"
                        + " WHERE u.id = ? AND u.status = 1 AND u.is_deleted = 0")
                .param(id).query((rs, row) -> new User(rs.getLong("id"), rs.getString("display_name"))).optional();
    }

    List<Option> userOptions(String search) {
        return jdbc.sql("SELECT u.id, " + USER_NAME + " AS display_name, u.username FROM sys_user u"
                        + " WHERE u.status = 1 AND u.is_deleted = 0 AND (u.username LIKE ? ESCAPE '!' OR u.nickname LIKE ? ESCAPE '!')"
                        + CAN_WORK
                        + " ORDER BY u.username, u.id LIMIT 50")
                .params(searchPattern(search), searchPattern(search), ROOT_ROLE, WORK)
                .query((rs, row) -> new Option(rs.getString("id"),
                        rs.getString("display_name") + "（" + rs.getString("username") + "）"))
                .list();
    }

    List<Option> roleOptions(String search) {
        return jdbc.sql("SELECT LOWER(r.code), r.name " + APPROVAL_ROLES
                        + " AND (r.code LIKE ? ESCAPE '!' OR r.name LIKE ? ESCAPE '!') ORDER BY r.name, r.id LIMIT 50")
                .params(ROOT_ROLE, WORK, searchPattern(search), searchPattern(search))
                .query((rs, row) -> new Option(rs.getString(1), rs.getString(2) + "（" + rs.getString(1) + "）"))
                .list();
    }

    boolean approvalRole(String code) {
        return jdbc.sql("SELECT COUNT(*) " + APPROVAL_ROLES + " AND LOWER(r.code) = ?")
                .params(ROOT_ROLE, WORK, code).query(Long.class).single() > 0;
    }

    Optional<User> approvalUser(long id) {
        return jdbc.sql("SELECT u.id, " + USER_NAME + " FROM sys_user u"
                        + " WHERE u.id = ? AND u.status = 1 AND u.is_deleted = 0 " + CAN_WORK)
                .params(id, ROOT_ROLE, WORK)
                .query((rs, n) -> new User(rs.getLong(1), rs.getString(2))).optional();
    }

    List<String> roles(long id) {
        return jdbc.sql("SELECT DISTINCT LOWER(r.code) " + ACTIVE_ROLES)
                .param(id).query(String.class).list();
    }

    boolean hasPermission(long userId, String permission) {
        return jdbc.sql("SELECT COUNT(*) " + ACTIVE_ROLES + """
                AND EXISTS (
                    SELECT 1 FROM sys_role_menu rm JOIN sys_menu m ON m.id = rm.menu_id
                    WHERE rm.role_id = r.id AND m.type = 4 AND m.perm = ?
                )
                """).params(userId, permission).query(Long.class).single() > 0;
    }

    Optional<ExistingRequest> existingRequest(String modelId, long applicantId, String requestId) {
        return jdbc.sql("""
                SELECT id, status FROM flow_approval_instance
                WHERE model_id = ? AND applicant_id = ? AND request_id = ?
                """).params(modelId, applicantId, requestId)
                .query((rs, n) -> new ExistingRequest(rs.getString(1), Status.DRAFT.name().equals(rs.getString(2))))
                .optional();
    }

    void insertInstance(String id, Definition definition, Start start, User applicant, String values, Status status) {
        jdbc.sql("""
                INSERT INTO flow_approval_instance (id, model_id, definition_id, definition_version, definition_name,
                    category, title, business_no, applicant_id, applicant_name, request_id, values_json, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """).params(id, definition.modelId(), definition.id(), definition.version(), definition.name(),
                        definition.category().name(), start.title().strip(), text(start.businessNo()),
                        applicant.id(), applicant.name(), start.requestId(), values, status.name())
                .update();
    }

    Optional<InstanceRow> instance(String id, boolean lock) {
        return jdbc.sql("SELECT " + INSTANCE_COLUMNS + ", values_json FROM flow_approval_instance WHERE id = ?"
                        + (lock ? " FOR UPDATE" : ""))
                .param(id).query((rs, row) -> new InstanceRow(INSTANCE.mapRow(rs, row), rs.getString("values_json")))
                .optional();
    }

    Optional<BusinessBinding> businessBinding(String instanceId) {
        return jdbc.sql("""
                SELECT business_page, business_id, active_business_id FROM flow_approval_business_binding WHERE instance_id = ?
                """).param(instanceId).query((rs, row) -> new BusinessBinding(rs.getString("business_page"),
                        rs.getString("business_id"), rs.getString("active_business_id") != null)).optional();
    }

    List<Instance> legacyPendingBusiness(String businessNo) {
        return jdbc.sql("SELECT " + INSTANCE_COLUMNS + """
                 FROM flow_approval_instance i
                 WHERE i.business_no = ? AND i.status IN ('PENDING', 'RUNNING')
                   AND NOT EXISTS (SELECT 1 FROM flow_approval_business_binding b WHERE b.instance_id = i.id)
                """).param(businessNo).query(INSTANCE).list();
    }

    void bindBusiness(String instanceId, String page, String businessId) {
        try {
            jdbc.sql("""
                    INSERT INTO flow_approval_business_binding (instance_id, business_page, business_id, active_business_id)
                    VALUES (?, ?, ?, ?)
                    """).params(instanceId, page, businessId, businessId).update();
        } catch (DuplicateKeyException exception) {
            throw new ApprovalException("该业务单据已有审批中的申请");
        }
    }

    void releaseBusiness(String instanceId, BusinessBinding binding) {
        int updated = jdbc.sql("""
                UPDATE flow_approval_business_binding SET active_business_id = NULL
                WHERE instance_id = ? AND business_page = ? AND active_business_id = ?
                """).params(instanceId, binding.page(), binding.businessId()).update();
        if (updated != 1) {
            throw new ApprovalException("业务单据的当前审批实例已变更");
        }
    }

    private record InboxSql(String aliases, String from, Map<String, Object> parameters) {}

    private InboxSql inboxSql(Inbox inbox, long userId, List<String> roles, String businessNo, InboxFilter filter) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("userId", userId);
        String joins = "";
        String condition;
        String inboxAt = "i.started_at";
        String comment = "NULL";
        String action = "NULL";
        String unread = "NULL";
        switch (inbox) {
            case TODO -> {
                parameters.put("assignee", Long.toString(userId));
                String candidate = "";
                if (!roles.isEmpty()) {
                    parameters.put("roles", roles);
                    candidate = """
                             OR (t.ASSIGNEE_ IS NULL AND EXISTS (SELECT 1 FROM ACT_RU_IDENTITYLINK l
                                 WHERE l.TASK_ID_ = t.ID_ AND l.TYPE_ = 'candidate' AND l.GROUP_ID_ IN (:roles)))
                            """;
                }
                joins = " JOIN ACT_RU_TASK t ON t.PROC_INST_ID_ = i.id AND t.SUSPENSION_STATE_ = 1";
                condition = "i.status IN ('PENDING', 'RUNNING') AND (t.ASSIGNEE_ = :assignee" + candidate + ")";
                inboxAt = "t.CREATE_TIME_";
            }
            case DONE -> {
                joins = """
                         JOIN flow_approval_action a ON a.id = (
                         SELECT MAX(a2.id) FROM flow_approval_action a2 WHERE a2.instance_id = i.id AND a2.actor_id = :userId
                             AND a2.action IN ('APPROVE', 'REJECT', 'SIGN_APPROVE', 'RETURN', 'TRANSFER', 'ADD_SIGN'))
                        """;
                condition = "1 = 1";
                inboxAt = "a.created_at";
                comment = "a.comment_text";
                action = "a.action";
            }
            case CC, REMIND -> {
                parameters.put("kind", inbox.name());
                joins = """
                         JOIN (
                             SELECT instance_id, MAX(created_at) AS latest, SUM(CASE WHEN is_read = FALSE THEN 1 ELSE 0 END) AS unread
                             FROM flow_approval_notice WHERE recipient_id = :userId AND kind = :kind GROUP BY instance_id
                         ) n ON n.instance_id = i.id
                        """;
                condition = "1 = 1";
                inboxAt = "n.latest";
                unread = "n.unread";
                if (filter.unread() != null) {
                    condition += filter.unread() ? " AND n.unread > 0" : " AND n.unread = 0";
                }
            }
            case MINE -> condition = "i.applicant_id = :userId";
            case ALL -> condition = "i.status <> 'DRAFT'";
            case ENDED -> condition = "i.status NOT IN ('DRAFT', 'PENDING', 'RUNNING')";
            default -> throw new IllegalArgumentException("未知审批列表");
        }

        if (!businessNo.isEmpty()) {
            condition += " AND i.business_no = :businessNo";
            parameters.put("businessNo", businessNo);
        }
        if (!text(filter.keyword()).isEmpty()) {
            condition += """
                     AND (LOWER(i.title) LIKE :keyword ESCAPE '!' OR LOWER(i.definition_name) LIKE :keyword ESCAPE '!'
                         OR LOWER(i.business_no) LIKE :keyword ESCAPE '!' OR LOWER(i.applicant_name) LIKE :keyword ESCAPE '!'
                         OR LOWER(i.id) LIKE :keyword ESCAPE '!')
                    """;
            parameters.put("keyword", searchPattern(filter.keyword()).toLowerCase(Locale.ROOT));
        }
        if (filter.category() != null) {
            condition += " AND i.category = :category";
            parameters.put("category", filter.category().name());
        }
        if (filter.status() != null) {
            condition += filter.status() == Status.PENDING ? " AND i.status IN ('PENDING', 'RUNNING')" : " AND i.status = :status";
            parameters.put("status", filter.status().name());
        }
        if (filter.from() != null) {
            condition += " AND " + inboxAt + " >= :from";
            parameters.put("from", Timestamp.from(filter.from()));
        }
        if (filter.until() != null) {
            condition += " AND " + inboxAt + " < :until";
            parameters.put("until", Timestamp.from(filter.until()));
        }
        String task = inbox == Inbox.TODO ? "t.ID_ AS inbox_task_id, t.NAME_ AS inbox_task_name"
                : "NULL AS inbox_task_id, NULL AS inbox_task_name";
        String aliases = inboxAt + " AS inbox_at, " + unread + " AS unread_count, "
                + comment + " AS activity_comment, " + action + " AS activity_action, " + task;
        String from = " FROM flow_approval_instance i" + joins + " WHERE " + condition + """
                 AND NOT EXISTS (
                    SELECT 1 FROM flow_approval_instance_meta m WHERE m.instance_id = i.id AND m.hidden = TRUE)
                """;
        return new InboxSql(aliases, from, parameters);
    }

    Page<Instance> instances(Inbox inbox, long userId, List<String> roles, int offset, int size, String businessNo, InboxFilter filter) {
        var scope = inboxSql(inbox, userId, roles, businessNo, filter);
        long count = jdbc.sql("SELECT COUNT(*)" + scope.from()).params(scope.parameters()).query(Long.class).single();
        String columns = "i." + INSTANCE_COLUMNS.replace(", ", ", i.");
        var list = jdbc.sql("SELECT " + columns + ", " + scope.aliases() + scope.from()
                        + " ORDER BY inbox_at DESC, i.id DESC LIMIT :size OFFSET :offset")
                .params(scope.parameters()).param("size", size).param("offset", offset)
                .query((rs, index) -> {
                    Instance row = INSTANCE.mapRow(rs, index);
                    Timestamp at = rs.getTimestamp("inbox_at");
                    String action = rs.getString("activity_action");
                    return new Instance(row.id(), row.definitionId(), row.definitionVersion(), row.definitionName(),
                            row.category(), row.title(), row.businessNo(), row.applicantId(), row.applicantName(), row.status(),
                            row.startedAt(), row.finishedAt(), rs.getString("inbox_task_id"), rs.getString("inbox_task_name"),
                            rs.getObject("unread_count", Integer.class), List.of(), at == null ? null : at.toInstant(),
                            rs.getString("activity_comment"), action == null ? null : ActionType.valueOf(action));
                }).list();
        return new Page<>(list, count);
    }

    InboxMetrics inboxMetrics(Inbox inbox, long userId, List<String> roles) {
        var scope = inboxSql(inbox, userId, roles, "", InboxFilter.EMPTY);
        String unread = inbox == Inbox.CC || inbox == Inbox.REMIND
                ? "SUM(CASE WHEN n.unread > 0 THEN 1 ELSE 0 END)" : "0";
        String waiting = inbox == Inbox.TODO
                ? "SUM(CASE WHEN t.CREATE_TIME_ <= :dayAgo THEN 1 ELSE 0 END)" : "0";
        var query = jdbc.sql("SELECT i.status, COUNT(*), " + unread + ", " + waiting + scope.from() + " GROUP BY i.status")
                .params(scope.parameters());
        if (inbox == Inbox.TODO) {
            query.param("dayAgo", Timestamp.from(Instant.now().minusSeconds(86400)));
        }
        Map<Status, Long> statuses = new EnumMap<>(Status.class);
        long[] counts = new long[3];
        query.query((rs, index) -> {
            Status status = "RUNNING".equals(rs.getString(1)) ? Status.PENDING : Status.valueOf(rs.getString(1));
            long total = rs.getLong(2);
            statuses.merge(status, total, Long::sum);
            counts[0] += total;
            counts[1] += rs.getLong(3);
            counts[2] += rs.getLong(4);
            return total;
        }).list();
        return new InboxMetrics(counts[0], statuses, counts[1], counts[2]);
    }

    int markNoticesRead(long userId, List<String> ids, NoticeKind kind) {
        if (ids != null && ids.isEmpty()) {
            return 0;
        }
        var query = jdbc.sql("UPDATE flow_approval_notice SET is_read = TRUE WHERE recipient_id = :userId"
                + " AND kind = :kind AND is_read = FALSE" + (ids == null ? "" : " AND instance_id IN (:ids)"))
                .param("userId", userId).param("kind", kind.name());
        return (ids == null ? query : query.param("ids", ids)).update();
    }

    record Meta(String source, String replacedBy, boolean hidden) {}
    record Signature(String task, long returnTo) {}

    Meta meta(String id) {
        return jdbc.sql("SELECT source_id, replaced_by, hidden FROM flow_approval_instance_meta WHERE instance_id = ?")
                .param(id).query((rs, n) -> new Meta(rs.getString(1), rs.getString(2), rs.getBoolean(3)))
                .optional().orElse(new Meta(null, null, false));
    }

    void link(String id, String source) {
        jdbc.sql("INSERT INTO flow_approval_instance_meta (instance_id, source_id) VALUES (?, ?)")
                .params(id, source).update();
    }

    void replace(String id, String replacement, boolean hide) {
        int changed = jdbc.sql("UPDATE flow_approval_instance_meta SET replaced_by = ?, hidden = ? WHERE instance_id = ?")
                .params(replacement, hide, id).update();
        if (changed == 0) {
            jdbc.sql("INSERT INTO flow_approval_instance_meta (instance_id, replaced_by, hidden) VALUES (?, ?, ?)")
                    .params(id, replacement, hide).update();
        }
    }

    void updateDraft(String id, Definition definition, Start start, String values) {
        jdbc.sql("""
                UPDATE flow_approval_instance SET definition_id = ?, definition_version = ?, definition_name = ?,
                category = ?, title = ?, business_no = ?, values_json = ? WHERE id = ?
                """).params(definition.id(), definition.version(), definition.name(), definition.category().name(),
                start.title().strip(), text(start.businessNo()), values, id).update();
    }

    Optional<Signature> signature(String id) {
        return jdbc.sql("SELECT task_id, return_to FROM flow_approval_signature WHERE instance_id = ?").param(id)
                .query((rs, n) -> new Signature(rs.getString(1), rs.getLong(2))).optional();
    }

    void addSignature(String id, String task, long returnTo) {
        jdbc.sql("INSERT INTO flow_approval_signature (instance_id, task_id, return_to) VALUES (?, ?, ?)")
                .params(id, task, returnTo).update();
    }

    void clearSignature(String id) {
        jdbc.sql("DELETE FROM flow_approval_signature WHERE instance_id = ?").param(id).update();
    }

    void notice(String id, String task, long recipient, NoticeKind kind) {
        jdbc.sql("INSERT INTO flow_approval_notice (id, instance_id, task_id, recipient_id, kind) VALUES (?, ?, ?, ?, ?)")
                .params(java.util.UUID.randomUUID().toString(), id, task, recipient, kind.name()).update();
    }

    boolean hasCc(String id, long user) {
        return jdbc.sql("SELECT COUNT(*) FROM flow_approval_notice WHERE instance_id = ? AND recipient_id = ? AND kind = 'CC'")
                .params(id, user).query(Integer.class).single() > 0;
    }

    boolean hasNotice(String id, long user) {
        return jdbc.sql("SELECT COUNT(*) FROM flow_approval_notice WHERE instance_id = ? AND recipient_id = ?")
                .params(id, user).query(Integer.class).single() > 0;
    }

    boolean recentlyReminded(String id, String task, long user) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM flow_approval_notice
                WHERE instance_id = ? AND task_id = ? AND recipient_id = ? AND kind = 'REMIND' AND created_at >= ?
                """).params(id, task, user, Timestamp.from(Instant.now().minusSeconds(300))).query(Integer.class).single() > 0;
    }

    int unread(String id, long user) {
        return jdbc.sql("SELECT COUNT(*) FROM flow_approval_notice WHERE instance_id = ? AND recipient_id = ? AND is_read = FALSE")
                .params(id, user).query(Integer.class).single();
    }

    void markRead(String id, long user) {
        jdbc.sql("UPDATE flow_approval_notice SET is_read = TRUE WHERE instance_id = ? AND recipient_id = ?")
                .params(id, user).update();
    }

    List<User> candidateUsers(List<String> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT DISTINCT u.id, " + USER_NAME + " AS display_name FROM sys_user u"
                        + " JOIN sys_user_role ur ON ur.user_id = u.id JOIN sys_role r ON r.id = ur.role_id"
                        + " WHERE u.status = 1 AND u.is_deleted = 0 AND r.status = 1 AND r.is_deleted = 0 AND LOWER(r.code) IN (:groups)")
                .param("groups", groups).query((rs, n) -> new User(rs.getLong(1), rs.getString(2))).list();
    }

    List<Attachment> attachments(String id) {
        return jdbc.sql("SELECT id, file_name, OCTET_LENGTH(data) FROM flow_approval_attachment WHERE instance_id = ? ORDER BY created_at, id")
                .param(id).query((rs, n) -> new Attachment(rs.getString(1), rs.getString(2), rs.getLong(3))).list();
    }

    String attach(String id, String name, byte[] data) {
        String attachment = java.util.UUID.randomUUID().toString();
        jdbc.sql("INSERT INTO flow_approval_attachment (id, instance_id, file_name, data) VALUES (?, ?, ?, ?)")
                .params(attachment, id, name, data).update();
        return attachment;
    }

    AttachmentData attachment(String instanceId, String id) {
        return jdbc.sql("SELECT file_name, data FROM flow_approval_attachment WHERE instance_id = ? AND id = ?")
                .params(instanceId, id).query((rs, n) -> new AttachmentData(rs.getString(1),
                        java.util.Base64.getEncoder().encodeToString(rs.getBytes(2))))
                .optional().orElseThrow(() -> new ApprovalException("附件不存在"));
    }

    void finish(String id, Status status) {
        jdbc.sql("UPDATE flow_approval_instance SET status = ?, finished_at = CURRENT_TIMESTAMP(3) WHERE id = ?")
                .params(status.name(), id).update();
    }

    void addAction(String instanceId, String taskId, String taskName, User user, ActionType action, String comment) {
        jdbc.sql("""
                INSERT INTO flow_approval_action (instance_id, task_id, task_name, actor_id, actor_name, action, comment_text)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """).params(instanceId, taskId, taskName, user.id(), user.name(), action.name(), text(comment))
                .update();
    }

    List<Action> actions(String id) {
        return jdbc.sql("""
                SELECT task_name, actor_id, actor_name, action, comment_text, created_at
                FROM flow_approval_action WHERE instance_id = ? ORDER BY id
                """).param(id).query((rs, row) -> new Action(rs.getString("task_name"), rs.getString("actor_id"),
                        rs.getString("actor_name"), ActionType.valueOf(rs.getString("action")), rs.getString("comment_text"),
                        instant(rs, "created_at"))).list();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    static String searchPattern(String value) {
        return "%" + text(value).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    static String text(String value) {
        return value == null ? "" : value.strip();
    }
}
