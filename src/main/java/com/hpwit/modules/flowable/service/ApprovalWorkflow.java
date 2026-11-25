package com.hpwit.modules.flowable.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hpwit.modules.flowable.model.ApprovalData.*;
import com.hpwit.modules.flowable.service.ApprovalAccess.Actor;
import com.hpwit.modules.flowable.service.ApprovalCallbacks.OperationEvent;
import com.hpwit.modules.flowable.service.BusinessApprovalStatusHandler.ApprovalReference;
import com.hpwit.modules.flowable.service.ApprovalStore.BusinessBinding;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.flowable.bpmn.model.UserTask;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER;
import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER_NO;
import static com.hpwit.modules.flowable.service.ApprovalAccess.*;
import static com.hpwit.modules.flowable.service.ApprovalBpmn.require;
import static com.hpwit.modules.flowable.service.ApprovalStore.text;

@Service
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
public class ApprovalWorkflow {

    private static final String ORDER_SNAPSHOT = "_order";
    private static final Order SAMPLE_ORDER = new Order("示例客户", "采购部", "2026-11-25", null,
            "", "", "", "", List.of(
                    new OrderItem("EQ-001", "显示器", 2, new BigDecimal("1250.00")),
                    new OrderItem("EQ-002", "扩展坞", 2, new BigDecimal("390.00"))));
    private static final Pattern DECIMAL = Pattern.compile("-?[0-9]{1,12}(?:\\.[0-9]{1,6})?");

    private final ApprovalStore store;
    private final ApprovalModels models;
    private final ApprovalAccess access;
    private final ProcessEngine engine;
    private final RuntimeService runtime;
    private final TaskService tasks;
    private final HistoryService history;
    private final Map<String, BusinessApprovalStatusHandler> businessStatusHandlers;
    private final ApprovalCallbacks callbacks;

    public ApprovalWorkflow(ApprovalStore store, ApprovalModels models, ApprovalAccess access, ProcessEngine engine,
                            List<BusinessApprovalStatusHandler> businessStatusHandlers, ApprovalCallbacks callbacks) {
        this.store = store;
        this.models = models;
        this.access = access;
        this.engine = engine;
        this.runtime = engine.getRuntimeService();
        this.tasks = engine.getTaskService();
        this.history = engine.getHistoryService();
        this.callbacks = callbacks;
        this.businessStatusHandlers = businessStatusHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(BusinessApprovalStatusHandler::businessPage, Function.identity()));
    }

    public Order order(Actor actor) {
        return store.order(actor.id())
                .map(json -> models.readJson(json, new TypeReference<Order>() {}))
                .orElse(SAMPLE_ORDER);
    }

    public OrderApproval orderApproval(Actor actor) {
        return store.orderApproval(actor.id()).orElseGet(() -> new OrderApproval(null, null));
    }

    public Order saveOrder(Actor actor, Order order) {
        try {
            var date = LocalDate.parse(order.date());
            require(order.deliveryDate() == null || !LocalDate.parse(order.deliveryDate()).isBefore(date),
                    "交付日期不能早于下单日期");
        } catch (DateTimeParseException invalidDate) {
            throw new ApprovalException("请填写有效的订单日期");
        }
        store.saveOrder(actor.id(), models.writeJson(order));
        return order;
    }

    // The model lock serializes submissions. Read the request after that lock without locking an instance row.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String start(Actor actor, Start form) {
        Definition definition = models.definition(form.definitionId());
        var model = models.model(definition.modelId(), true).summary();
        var previous = store.existingRequest(model.id(), actor.id(), form.requestId());
        if (previous.isPresent()) {
            require(!previous.get().draft(), "请求已保存为草稿，请从我的申请提交");
            return previous.get().id();
        }
        require(definition.id().equals(model.definitionId()), "此流程已有新版本，请重新选择并填写申请");
        String id = createInstance(actor, form, definition, submissionOrder(actor, definition), null);
        notifyOperation(id, null, actor, ActionType.START, null, null);
        return id;
    }

    private record FormValues(Map<String, Object> stored, Map<String, Object> variables) {}

    private Order submissionOrder(Actor actor, Definition definition) {
        return DEMO_ORDER.equals(definition.businessPage()) ? order(actor) : null;
    }

    private Order removeOrderSnapshot(Map<String, JsonNode> values, Definition definition) {
        if (!DEMO_ORDER.equals(definition.businessPage())) {
            return null;
        }
        var snapshot = values.remove(ORDER_SNAPSHOT);
        return snapshot == null ? SAMPLE_ORDER : models.readJson(snapshot.toString(), new TypeReference<Order>() {});
    }

    private FormValues validateForm(Definition definition, Start form, boolean submitting, Order order) {
        String businessNo = text(form.businessNo());
        if (order != null) {
            require(DEMO_ORDER_NO.equals(businessNo), "请使用测试订单 " + DEMO_ORDER_NO + " 发起申请");
        } else {
            require(!DEMO_ORDER_NO.equalsIgnoreCase(businessNo), "请使用关联测试订单详情的流程");
        }

        require(definition.businessPage() == null || !businessNo.isEmpty(), "请填写关联业务单号");
        var fieldKeys = definition.fields().stream().map(Field::key).collect(Collectors.toSet());
        require(fieldKeys.containsAll(form.values().keySet()), "申请包含未定义的字段");
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, Object> variables = new HashMap<>();
        for (Field field : definition.fields()) {
            JsonNode value = order != null && "amount".equals(field.key())
                    ? TextNode.valueOf(order.amount().toPlainString())
                    : form.values().get(field.key());
            if (value == null || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
                require(!submitting || !field.required(), "请填写" + field.label());
                continue;
            }
            variables.put(field.key(), variable(field, value));
            values.put(field.key(), value);
        }
        if (order != null) {
            values.put(ORDER_SNAPSHOT, order);
        }
        return new FormValues(values, variables);
    }

    private String createInstance(Actor actor, Start form, Definition definition, Order order, BusinessBinding previousBinding) {
        var values = validateForm(definition, form, true, order);
        String businessNo = text(form.businessNo());
        String businessPage = definition.businessPage();
        var handler = businessPage == null ? null : businessStatusHandlers.get(businessPage);
        String businessId = businessPage == null ? null
                : resolveSubmissionBusinessId(actor, businessNo, businessPage, handler, previousBinding);
        values.variables().put("applicantId", actor.engineId());

        String previousUser = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId(actor.engineId());
        try {
            var builder = runtime.createProcessInstanceBuilder().processDefinitionId(definition.id()).variables(values.variables())
                    .name(form.title().strip());
            if (!businessNo.isEmpty()) {
                builder.businessKey(businessNo);
            }
            String id = builder.start().getId();
            requireTaskRecipient(currentTask(id));
            store.insertInstance(id, definition, form, actor.user(), models.writeJson(values.stored()), Status.PENDING);
            if (businessId != null) {
                store.bindBusiness(id, businessPage, businessId);
                syncBusinessStatus(definition, businessNo, id, actor.id(), businessId, Status.PENDING);
            }
            store.addAction(id, null, null, actor.user(), ActionType.START, null);
            return id;
        } finally {
            Authentication.setAuthenticatedUserId(previousUser);
        }
    }

    private String resolveSubmissionBusinessId(Actor actor, String businessNo, String businessPage,
                                               BusinessApprovalStatusHandler handler, BusinessBinding previousBinding) {
        String businessId = handler == null ? businessNo : handler.resolveBusinessId(businessNo, actor.id());
        require(businessId != null && !businessId.isBlank() && businessId.length() <= 191, "无法定位业务单据");
        require(previousBinding == null || previousBinding.page().equals(businessPage)
                        && previousBinding.businessId().equals(businessId),
                "业务单据已变更，请从业务页面重新发起审批");
        for (var pending : store.legacyPendingBusiness(businessNo)) {
            if (businessPage.equals(models.definition(pending.definitionId()).businessPage())) {
                String pendingBusinessId = handler == null ? pending.businessNo()
                        : handler.legacyBusinessId(pending.businessNo(), Long.parseLong(pending.applicantId()));
                require(pendingBusinessId != null && !businessId.equals(pendingBusinessId), "该业务单据已有审批中的申请");
            }
        }
        return businessId;
    }

    private void finish(Instance instance, Status status) {
        store.finish(instance.id(), status);
        if (!instance.businessNo().isEmpty()) {
            var definition = models.definition(instance.definitionId());
            var binding = store.businessBinding(instance.id());
            binding.ifPresent(value -> store.releaseBusiness(instance.id(), value));
            syncBusinessStatus(definition, instance.businessNo(), instance.id(), Long.parseLong(instance.applicantId()),
                    binding.map(BusinessBinding::businessId).orElse(null), status);
        }
    }

    private void syncBusinessStatus(Definition definition, String businessNo, String instanceId, long applicantId,
                                    String businessId, Status status) {
        String businessPage = definition.businessPage();
        if (businessPage == null) {
            return;
        }
        var handler = businessStatusHandlers.get(businessPage);
        var config = definition.statusCallback();
        Integer mappedStatus = config == null ? null : config.statusMapping().get(status);
        var approval = new ApprovalReference(businessNo, instanceId, applicantId, businessId, mappedStatus);
        if (handler != null) {
            handler.updateStatus(approval, status);
        } else {
            require(mappedStatus == null, "此业务页面的同库回写处理器未启用");
        }
    }

    @Transactional(readOnly = true)
    public Page<Instance> list(Actor actor, Inbox inbox, int offset, int size) {
        return list(actor, inbox, offset, size, null, InboxFilter.EMPTY);
    }

    @Transactional(readOnly = true)
    public Page<Instance> list(Actor actor, Inbox inbox, int offset, int size, String businessNo) {
        return list(actor, inbox, offset, size, businessNo, InboxFilter.EMPTY);
    }

    @Transactional(readOnly = true)
    public Page<Instance> list(Actor actor, Inbox inbox, int offset, int size, String businessNo, InboxFilter filter) {
        require(text(businessNo).isEmpty() || inbox == Inbox.MINE, "业务单号筛选适用于我的申请");
        require(filter.from() == null || filter.until() == null || filter.from().isBefore(filter.until()),
                "结束时间须晚于开始时间");
        boolean managerialInbox = inbox == Inbox.ALL || inbox == Inbox.ENDED;
        boolean admin = managerialInbox && access.can(actor, ADMIN);
        require(managerialInbox ? admin || access.can(actor, MONITOR) : access.can(actor, WORK),
                "当前用户没有查看此审批列表的权限");

        var page = store.instances(inbox, actor.id(), actor.roles(), offset, size, text(businessNo), filter);
        if (!admin && inbox != Inbox.MINE && inbox != Inbox.ENDED) {
            return page;
        }
        return new Page<>(page.list().stream().map(item -> {
            Task task = item.status() == Status.PENDING ? currentTask(item.id()) : null;
            var operations = new ArrayList<>(operations(actor, item, task, false));
            if (task != null && moveTargets(item, task).isEmpty()) {
                operations.remove(Operation.MOVE);
            }
            return item.withTask(task == null ? null : task.getId(), task == null ? null : task.getName())
                    .withListOperations(operations);
        }).toList(), page.total());
    }

    @Transactional(readOnly = true)
    public Overview overview(Actor actor) {
        require(access.can(actor, WORK), "当前用户没有审批中心权限");
        var metrics = new EnumMap<Inbox, InboxMetrics>(Inbox.class);
        for (var inbox : List.of(Inbox.TODO, Inbox.MINE, Inbox.DONE, Inbox.CC, Inbox.REMIND)) {
            metrics.put(inbox, store.inboxMetrics(inbox, actor.id(), actor.roles()));
        }
        return new Overview(metrics);
    }

    @Transactional
    public void batch(Actor actor, BatchCommand form) {
        require(Set.of(Operation.APPROVE, Operation.REJECT, Operation.TRANSFER).contains(form.operation()),
                "不支持该批量操作");
        var ids = new HashSet<String>();
        require(form.items().stream().allMatch(item -> ids.add(item.instanceId())), "批量操作包含重复申请");
        form.items().stream().sorted(Comparator.comparing(TaskRef::instanceId)).forEach(item ->
                command(actor, item.instanceId(),
                        new Command(form.operation(), item.taskId(), form.userId(), null, form.comment())));
    }

    @Transactional
    public int markNoticesRead(Actor actor, ReadNotices form, NoticeKind kind) {
        require(access.can(actor, WORK), "当前用户没有审批中心权限");
        return store.markNoticesRead(actor.id(), form.instanceIds(), kind);
    }

    @Transactional(readOnly = true)
    public Detail detail(Actor actor, String id) {
        var row = instance(id, false);
        var instance = row.instance();
        boolean applicant = actor.engineId().equals(instance.applicantId());
        boolean running = instance.status() == Status.PENDING;
        Task task = running ? currentTask(id) : null;
        boolean workPermission = access.can(actor, ApprovalAccess.WORK);
        boolean canApprove = workPermission && eligible(task, actor);
        var actions = store.actions(id);
        require(canView(actor, row, canApprove, actions), "当前用户无权查看此申请");
        var operations = operations(actor, instance, task, canApprove);
        var activities = history.createHistoricActivityInstanceQuery().processInstanceId(id)
                .orderByHistoricActivityInstanceStartTime().asc().list().stream()
                .filter(activity -> !"sequenceFlow".equals(activity.getActivityType()))
                .map(activity -> new Activity(activity.getActivityId(), activity.getActivityName(), activity.getActivityType(),
                        activity.getEndTime() == null && running, activity.getDeleteReason() != null, activity.getStartTime().toInstant(),
                        activity.getEndTime() == null ? null : activity.getEndTime().toInstant()))
                .toList();
        var definition = models.definition(instance.definitionId());
        Map<String, JsonNode> values = models.readJson(row.values(), new TypeReference<>() {});
        var order = removeOrderSnapshot(values, definition);
        return new Detail(task == null ? instance : instance.withTask(task.getId(), task.getName()), definition.fields(),
                values, models.diagram(definition.id()),
                activities, actions, canApprove, running && applicant && workPermission, definition.businessPage(),
                definition.pageConfig(), operations, store.attachments(id),
                operations.contains(Operation.MOVE) ? moveTargets(instance, task) : List.of(), store.meta(id).source(), order,
                callbacks.delivery(definition.statusCallback(), id));
    }

    private boolean canView(Actor actor, ApprovalStore.InstanceRow row, boolean eligible, List<Action> actions) {
        var item = row.instance();
        if (item.status() == Status.DRAFT) {
            return item.applicantId().equals(actor.engineId()) && access.can(actor, WORK);
        }
        boolean participant = actions.stream().anyMatch(action -> action.actorId().equals(actor.engineId()));
        return access.can(actor, MONITOR) || access.can(actor, ADMIN) || access.can(actor, WORK)
                && (item.applicantId().equals(actor.engineId()) || eligible || participant || store.hasNotice(item.id(), actor.id()));
    }

    private List<Operation> operations(Actor actor, Instance item, Task task, boolean approver) {
        var result = new ArrayList<Operation>();
        boolean work = access.can(actor, WORK);
        boolean owner = work && item.applicantId().equals(actor.engineId());
        boolean admin = access.can(actor, ADMIN);
        var status = item.status();
        boolean available = store.meta(item.id()).replacedBy() == null;
        if (owner && status == Status.DRAFT) {
            result.addAll(List.of(Operation.EDIT, Operation.RESUBMIT, Operation.DELETE));
        }
        if (owner && (status == Status.REJECTED || status == Status.RETURNED) && available) {
            result.addAll(List.of(Operation.EDIT, Operation.RESUBMIT));
        }
        if (owner && (status == Status.APPROVED || status == Status.WITHDRAWN || status == Status.TERMINATED) && available) {
            result.add(Operation.RESUBMIT);
        }
        if (admin && status == Status.TERMINATED && available) {
            result.add(Operation.RESTART);
        }
        if (status == Status.PENDING && task != null) {
            if (owner) {
                result.addAll(List.of(Operation.WITHDRAW, Operation.CANCEL, Operation.REMIND));
            }
            if (approver) {
                result.addAll(List.of(Operation.APPROVE, Operation.REJECT, Operation.TRANSFER, Operation.RETURN));
                if (store.signature(item.id()).isEmpty()) {
                    result.add(Operation.ADD_SIGN);
                }
            }
            if (admin) {
                result.addAll(List.of(Operation.FORCE_APPROVE, Operation.FORCE_REJECT, Operation.TERMINATE,
                        Operation.ASSIGN, Operation.MOVE));
            }
            if (owner || approver || admin) {
                result.addAll(List.of(Operation.COMMENT, Operation.ATTACH, Operation.CC));
            }
        }
        if (work && store.unread(item.id(), actor.id()) > 0) {
            result.add(Operation.MARK_READ);
        }
        return result;
    }

    @Transactional
    public void decide(Actor actor, String id, String taskId, boolean approved, String comment) {
        command(actor, id, new Command(approved ? Operation.APPROVE : Operation.REJECT, taskId, null, null, comment));
    }

    @Transactional
    public void withdraw(Actor actor, String id, String comment) {
        command(actor, id, new Command(Operation.WITHDRAW, null, null, null, comment));
    }

    @Transactional
    public String command(Actor actor, String id, Command form) {
        var row = instance(id, true);
        var item = row.instance();
        Task task = item.status() == Status.PENDING ? currentTask(id) : null;
        boolean approver = access.can(actor, WORK) && eligible(task, actor);
        var operation = form.operation();
        require(operations(actor, item, task, approver).contains(operation), "操作不可用，请刷新并检查权限");
        if (Set.of(Operation.APPROVE, Operation.REJECT, Operation.TRANSFER, Operation.ADD_SIGN, Operation.RETURN,
                Operation.ASSIGN, Operation.MOVE, Operation.FORCE_APPROVE, Operation.FORCE_REJECT,
                Operation.TERMINATE).contains(operation)) {
            require(task != null && task.getId().equals(form.taskId()), "该待办已变更，请刷新列表");
        }
        String comment = text(form.comment());
        if (Set.of(Operation.REJECT, Operation.RETURN, Operation.FORCE_APPROVE, Operation.FORCE_REJECT,
                Operation.TERMINATE, Operation.MOVE, Operation.COMMENT).contains(operation)) {
            require(!comment.isEmpty(), "请填写处理原因或评论");
        }
        ActionType action = null;
        switch (operation) {
            case APPROVE -> {
                var signature = store.signature(id);
                if (signature.isPresent()) {
                    require(task.getId().equals(signature.get().task()), "加签任务已变更");
                    require(store.approvalUser(signature.get().returnTo()).isPresent(),
                            "原审批人已不可用，请联系流程管理员重新指定审批人");
                    tasks.setAssignee(task.getId(), Long.toString(signature.get().returnTo()));
                    store.clearSignature(id);
                    action = ActionType.SIGN_APPROVE;
                } else {
                    if (task.getAssignee() == null) {
                        tasks.claim(task.getId(), actor.engineId());
                    }
                    tasks.complete(task.getId());
                    if (runtime.createProcessInstanceQuery().processInstanceId(id).count() == 0) {
                        finish(item, Status.APPROVED);
                    } else {
                        requireTaskRecipient(currentTask(id));
                    }
                    action = ActionType.APPROVE;
                }
            }
            case REJECT, RETURN, FORCE_REJECT, FORCE_APPROVE, TERMINATE, WITHDRAW, CANCEL -> {
                Status status = switch (operation) {
                    case FORCE_APPROVE -> Status.APPROVED;
                    case TERMINATE -> Status.TERMINATED;
                    case WITHDRAW -> Status.WITHDRAWN;
                    case CANCEL -> Status.CANCELLED;
                    case RETURN -> Status.RETURNED;
                    default -> Status.REJECTED;
                };
                runtime.deleteProcessInstance(id, operation.name());
                finish(item, status);
                store.clearSignature(id);
                action = ActionType.valueOf(operation.name());
            }
            case TRANSFER, ADD_SIGN, ASSIGN -> {
                var user = recipient(form.userId());
                require(!user.engineId().equals(task.getAssignee()) && (operation == Operation.ASSIGN || user.id() != actor.id()),
                        "请选择其他审批人");
                if (operation == Operation.ADD_SIGN) {
                    store.addSignature(id, task.getId(), actor.id());
                } else if (operation == Operation.ASSIGN) {
                    store.clearSignature(id);
                }
                tasks.setAssignee(task.getId(), user.engineId());
                comment = withTarget(user.name(), comment);
                action = ActionType.valueOf(operation.name());
            }
            case MOVE -> {
                var target = moveTargets(item, task).stream().filter(node -> node.value().equals(form.nodeId()))
                        .findFirst().orElseThrow(() -> new ApprovalException("目标节点不可用"));
                runtime.createChangeActivityStateBuilder().processInstanceId(id)
                        .moveExecutionToActivityId(task.getExecutionId(), target.value()).changeState();
                requireTaskRecipient(currentTask(id));
                store.clearSignature(id);
                comment = withTarget(target.label(), comment);
                action = ActionType.MOVE;
            }
            case CC -> {
                var user = recipient(form.userId());
                require(!store.hasCc(id, user.id()), "已抄送该用户");
                store.notice(id, task.getId(), user.id(), NoticeKind.CC);
                comment = withTarget(user.name(), comment);
                action = ActionType.CC;
            }
            case REMIND -> {
                int sent = 0;
                for (var user : taskRecipients(task)) {
                    if (!store.recentlyReminded(id, task.getId(), user.id())) {
                        store.notice(id, task.getId(), user.id(), NoticeKind.REMIND);
                        sent++;
                    }
                }
                require(sent > 0, "没有可提醒的审批人，或近五分钟内已催办");
                comment = withTarget("已提醒 " + sent + " 人", comment);
                action = ActionType.REMIND;
            }
            case COMMENT -> action = ActionType.COMMENT;
            case MARK_READ -> store.markRead(id, actor.id());
            case DELETE -> {
                var source = store.meta(id).source();
                store.replace(id, null, true);
                if (source != null && item.status() == Status.DRAFT) {
                    store.replace(source, null, false);
                }
                action = ActionType.DELETE;
            }
            case RESTART -> {
                long applicantId = Long.parseLong(item.applicantId());
                var user = store.user(applicantId).orElseThrow(() -> new ApprovalException("原发起人不可用"));
                var owner = new Actor(user.id(), user.name(), store.roles(user.id()));
                require(access.can(owner, WORK), "原发起人没有审批工作台权限");
                var definition = models.definition(item.definitionId());
                Map<String, JsonNode> values = models.readJson(row.values(), new TypeReference<>() {});
                var order = removeOrderSnapshot(values, definition);
                var request = new Start(item.definitionId(), UUID.randomUUID().toString(), item.title(), item.businessNo(), values);
                String next = createInstance(owner, request, definition, order, store.businessBinding(id).orElse(null));
                store.link(next, id);
                store.replace(id, next, false);
                store.addAction(id, null, null, actor.user(), ActionType.RESTART, withTarget(next, comment));
                store.addAction(next, null, null, actor.user(), ActionType.RESTART, withTarget(id, comment));
                notifyOperation(next, null, actor, ActionType.RESTART, null, id);
                return next;
            }
            default -> throw new ApprovalException("请使用对应的表单或上传接口");
        }
        if (action != null) {
            record(id, task, actor, action, comment);
            notifyOperation(id, task, actor, action, form.userId(), null);
        }
        return null;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String saveDraft(Actor actor, String id, Start form) {
        require(access.can(actor, WORK), "当前用户没有审批工作台权限");
        String draftId = id;
        if (id == null) {
            var definition = models.definition(form.definitionId());
            var model = models.model(definition.modelId(), true).summary();
            var previous = store.existingRequest(model.id(), actor.id(), form.requestId());
            if (previous.isPresent()) {
                var existing = previous.get();
                require(existing.draft() && !store.meta(existing.id()).hidden(), "此请求已提交或已删除");
                return existing.id();
            }
            require(definition.id().equals(model.definitionId()), "此流程已有新版本，请重新选择并填写申请");
            var values = models.writeJson(validateForm(definition, form, false, submissionOrder(actor, definition)).stored());
            draftId = UUID.randomUUID().toString();
            store.insertInstance(draftId, definition, form, actor.user(), values, Status.DRAFT);
        } else {
            var row = instance(id, true);
            require(row.instance().applicantId().equals(actor.engineId()), "仅发起人可编辑申请");
            require(store.meta(id).replacedBy() == null, "申请已重新提交");
            require(Set.of(Status.DRAFT, Status.REJECTED, Status.RETURNED).contains(row.instance().status()),
                    "仅草稿、已驳回或已退回的申请可以编辑");
            require(row.instance().businessNo().equals(text(form.businessNo())), "不能更改原业务单号");
            var definition = resubmissionDefinition(row.instance(), form);
            Map<String, JsonNode> previousValues = models.readJson(row.values(), new TypeReference<>() {});
            var order = removeOrderSnapshot(previousValues, definition);
            var values = models.writeJson(validateForm(definition, form, false, order).stored());
            if (row.instance().status() == Status.DRAFT) {
                store.updateDraft(id, definition, form, values);
            } else {
                draftId = UUID.randomUUID().toString();
                var draft = new Start(form.definitionId(), UUID.randomUUID().toString(), form.title(), form.businessNo(), form.values());
                store.insertInstance(draftId, definition, draft, actor.user(), values, Status.DRAFT);
                store.link(draftId, id);
                store.replace(id, draftId, false);
            }
        }
        store.addAction(draftId, null, null, actor.user(), ActionType.SAVE_DRAFT, null);
        notifyOperation(draftId, null, actor, ActionType.SAVE_DRAFT, null, id != null && !id.equals(draftId) ? id : null);
        return draftId;
    }

    @Transactional
    public String resubmit(Actor actor, String id, Start form) {
        var row = store.instance(id, true).orElseThrow(() -> new ApprovalException("审批申请不存在"));
        var item = row.instance();
        require(access.can(actor, WORK) && item.applicantId().equals(actor.engineId()), "仅发起人可重新提交");
        var meta = store.meta(id);
        if (meta.replacedBy() != null && !store.meta(meta.replacedBy()).hidden()) {
            var replacement = store.instance(meta.replacedBy(), false);
            if (replacement.isPresent() && replacement.get().instance().status() != Status.DRAFT) {
                return meta.replacedBy();
            }
        }
        require(!meta.hidden() && operations(actor, item, null, false).contains(Operation.RESUBMIT), "当前申请不能重新提交");
        require(item.businessNo().equals(text(form.businessNo())), "不能更改原业务单号");
        var definition = resubmissionDefinition(item, form);
        var nextRequest = new Start(form.definitionId(), UUID.randomUUID().toString(), form.title(), form.businessNo(), form.values());
        Map<String, JsonNode> previousValues = models.readJson(row.values(), new TypeReference<>() {});
        String sourceId = item.status() == Status.DRAFT && meta.source() != null ? meta.source() : id;
        var source = sourceId.equals(id) ? item : instance(sourceId, false).instance();
        var order = source.status() == Status.RETURNED ? submissionOrder(actor, definition)
                : removeOrderSnapshot(previousValues, definition);
        String next = createInstance(actor, nextRequest, definition, order, store.businessBinding(sourceId).orElse(null));
        store.link(next, sourceId);
        store.replace(id, next, item.status() == Status.DRAFT);
        if (!sourceId.equals(id)) {
            store.replace(meta.source(), next, false);
        }
        store.addAction(next, null, null, actor.user(), ActionType.RESUBMIT, "来源申请 " + sourceId);
        notifyOperation(next, null, actor, ActionType.RESUBMIT, null, sourceId);
        return next;
    }

    private Definition resubmissionDefinition(Instance item, Start form) {
        var definition = models.resubmissionDefinition(item.definitionId(), true);
        require(definition.id().equals(form.definitionId()), "流程版本已变更，请重新打开申请");
        return definition;
    }

    @Transactional
    public void attach(Actor actor, String id, String name, byte[] bytes) {
        var item = instance(id, true).instance();
        var task = item.status() == Status.PENDING ? currentTask(id) : null;
        require(operations(actor, item, task, access.can(actor, WORK) && eligible(task, actor)).contains(Operation.ATTACH),
                "当前用户无法上传附件");
        require(bytes.length > 0 && bytes.length <= 2 * 1024 * 1024 && store.attachments(id).size() < 10,
                "每个申请最多上传 10 个附件，每个最大 2 MB");
        require(name != null && name.matches("[^\\p{Cntrl}/\\\\]{1,200}"), "附件名称无效");
        store.attach(id, name, bytes);
        record(id, task, actor, ActionType.ATTACH, name);
        notifyOperation(id, task, actor, ActionType.ATTACH, null, null);
    }

    @Transactional(readOnly = true)
    public AttachmentData attachment(Actor actor, String id, String attachment) {
        var row = instance(id, false);
        boolean eligible = row.instance().status() == Status.PENDING && eligible(currentTask(id), actor);
        require(canView(actor, row, eligible, store.actions(id)), "当前用户无权查看此申请");
        return store.attachment(id, attachment);
    }

    public Definition editableDefinition(Actor actor, String id) {
        var item = instance(id, false).instance();
        require(operations(actor, item, null, false).contains(Operation.RESUBMIT), "当前申请不能重新提交");
        return models.resubmissionDefinition(item.definitionId(), false);
    }

    public List<Option> recipients(String search) {
        return store.userOptions(search);
    }

    private ApprovalStore.User recipient(String id) {
        try {
            long userId = Long.parseLong(id);
            return store.approvalUser(userId)
                    .orElseThrow(() -> new ApprovalException("指定用户不可用或没有审批中心权限"));
        } catch (NumberFormatException e) {
            throw new ApprovalException("请选择用户");
        }
    }

    private List<ApprovalStore.User> taskRecipients(Task task) {
        if (task.getAssignee() != null) {
            return store.approvalUser(Long.parseLong(task.getAssignee())).stream().toList();
        }
        var groups = tasks.getIdentityLinksForTask(task.getId()).stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                .map(link -> link.getGroupId()).toList();
        return store.candidateUsers(groups).stream()
                .filter(user -> access.can(new Actor(user.id(), user.name(), store.roles(user.id())), WORK)).toList();
    }

    private void requireTaskRecipient(Task task) {
        require(task != null, "流程未进入可办理的审批节点");
        require(!taskRecipients(task).isEmpty(), "节点「" + task.getName() + "」没有可办理的审批人，请联系流程管理员");
    }

    private List<Option> moveTargets(Instance instance, Task task) {
        return engine.getRepositoryService().getBpmnModel(instance.definitionId()).getMainProcess()
                .findFlowElementsOfType(UserTask.class).stream()
                .filter(node -> !node.getId().equals(task.getTaskDefinitionKey()))
                .map(node -> new Option(node.getId(), node.getName())).toList();
    }

    private void record(String id, Task task, Actor actor, ActionType action, String comment) {
        store.addAction(id, task == null ? null : task.getId(), task == null ? null : task.getName(), actor.user(), action, comment);
    }

    private void notifyOperation(String id, Task task, Actor actor, ActionType action, String targetUserId, String sourceId) {
        var instance = store.instance(id, false).orElseThrow().instance();
        if (instance.businessNo().isEmpty()) {
            return;
        }
        var definition = models.definition(instance.definitionId());
        var config = definition.statusCallback();
        if (config == null || !config.operationEndpoints().containsKey(action)) {
            return;
        }
        var binding = store.businessBinding(id).orElse(null);
        var approval = new ApprovalReference(instance.businessNo(), id, Long.parseLong(instance.applicantId()),
                binding == null ? null : binding.businessId(), null);
        Task next = instance.status() == Status.PENDING ? currentTask(id) : null;
        var event = new OperationEvent(action, actor.engineId(), task == null ? null : task.getId(),
                task == null ? null : task.getName(), task == null ? null : task.getTaskDefinitionKey(),
                next == null ? null : next.getId(), next == null ? null : next.getTaskDefinitionKey(),
                targetUserId == null && next != null ? next.getAssignee() : targetUserId, sourceId);
        callbacks.enqueueOperation(definition.businessPage(), approval, instance.status(), config, event);
    }

    private String withTarget(String target, String comment) {
        String value = target + (comment.isEmpty() ? "" : " · " + comment);
        return value.substring(0, Math.min(1000, value.length()));
    }

    private ApprovalStore.InstanceRow instance(String id, boolean lock) {
        var row = store.instance(id, lock).orElseThrow(() -> new ApprovalException("审批申请不存在"));
        require(!store.meta(id).hidden(), "审批申请已删除或已提交");
        return row;
    }

    private Task currentTask(String id) {
        return tasks.createTaskQuery().active().processInstanceId(id).singleResult();
    }

    private boolean eligible(Task task, Actor actor) {
        if (task == null) {
            return false;
        }
        if (task.getAssignee() != null) {
            return task.getAssignee().equals(actor.engineId());
        }
        return tasks.getIdentityLinksForTask(task.getId()).stream().anyMatch(link ->
                "candidate".equals(link.getType()) && link.getGroupId() != null && actor.roles().contains(link.getGroupId()));
    }

    private Object variable(Field field, JsonNode value) {
        String invalid = field.label() + "的值无效";
        switch (field.type()) {
            case NUMBER -> {
                require(value.isNumber() || (value.isTextual() && DECIMAL.matcher(value.textValue()).matches()), invalid);
                BigDecimal number = (value.isNumber() ? value.decimalValue() : new BigDecimal(value.textValue()))
                        .stripTrailingZeros();
                require(number.scale() <= 6 && (long) number.precision() - number.scale() <= 12, invalid);
                return number;
            }
            case BOOLEAN -> {
                require(value.isBoolean(), invalid);
                return value.booleanValue();
            }
            case DATE -> {
                require(value.isTextual() && value.asText().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"), invalid);
                try {
                    LocalDate.parse(value.asText());
                    return value.asText();
                } catch (DateTimeParseException e) {
                    throw new ApprovalException(invalid);
                }
            }
            case TEXT, TEXTAREA -> {
                require(value.isTextual() && value.asText().length() <= (field.type() == FieldType.TEXT ? 200 : 1000), invalid);
                return value.asText().strip();
            }
            default -> throw new IllegalStateException();
        }
    }
}
