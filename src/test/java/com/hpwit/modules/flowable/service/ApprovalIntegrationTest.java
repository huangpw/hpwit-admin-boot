package com.hpwit.modules.flowable.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import com.hpwit.modules.flowable.config.FlowableConfig;
import com.hpwit.modules.flowable.model.ApprovalData.*;
import com.hpwit.modules.flowable.service.ApprovalAccess.Actor;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER;
import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER_NO;
import static org.assertj.core.api.Assertions.*;

class ApprovalIntegrationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(FlowableConfig.class, ApprovalStore.class, ApprovalModels.class,
                    ApprovalAccess.class, ApprovalWorkflow.class, ApprovalPages.class, OrderApprovalStatusHandler.class,
                    ApprovalCallbacks.class);

    @Test
    void engineAndServicesAreAbsentByDefault() {
        context.run(ctx -> assertThat(ctx).doesNotHaveBean(ProcessEngine.class).doesNotHaveBean(ApprovalWorkflow.class));
    }

    @Test
    void publishesVersionsAndProcessesEachApprovalOutcome() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    var models = ctx.getBean(ApprovalModels.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var applicant = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("root", "finance"));
                    var other = new Actor(3, "Other", List.of("employee"));
                    var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                    var draft = models.save(null, new SaveModel("采购 V1", Category.PURCHASE, "", BPMN, fields, null, null));
                    var first = models.publish(draft.model().id(), 1);
                    assertThat(models.publish(draft.model().id(), 1).id()).isEqualTo(first.id());

                    var form = form(first, "1500.10");
                    String approved = workflow.start(applicant, form);
                    assertThat(workflow.start(applicant, form)).isEqualTo(approved);
                    assertThat(workflow.list(reviewer, Inbox.TODO, 0, 10).total()).isEqualTo(1);
                    assertThat(workflow.list(applicant, Inbox.TODO, 0, 10).total()).isZero();
                    assertThatThrownBy(() -> workflow.detail(other, approved)).isInstanceOf(ApprovalException.class);
                    var current = workflow.detail(reviewer, approved);
                    assertThat(current.instance().taskName()).isEqualTo("财务审批");
                    assertThat(current.activities()).anyMatch(activity -> activity.active() && activity.id().equals("finance"));
                    assertThat(current.canApprove()).isTrue();
                    assertThat(current.canWithdraw()).isFalse();
                    assertThatThrownBy(() -> workflow.decide(other, approved, current.instance().taskId(), true, ""))
                            .isInstanceOf(ApprovalException.class);

                    var updated = models.save(draft.model().id(),
                            new SaveModel("采购 V2", Category.PURCHASE, "", BPMN, fields, 1, null));
                    var second = models.publish(updated.model().id(), updated.model().revision());
                    assertThat(second.version()).isEqualTo(first.version() + 1);
                    assertThat(models.definitions()).containsExactly(second);
                    assertThatThrownBy(() -> workflow.start(applicant, form(first, "99")))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("新版本");

                    workflow.decide(reviewer, approved, current.instance().taskId(), true, "同意");
                    var next = workflow.detail(reviewer, approved);
                    assertThat(next.instance().status()).isEqualTo(Status.PENDING);
                    assertThat(next.instance().taskName()).isEqualTo("主管审批");
                    assertThatThrownBy(() -> workflow.decide(reviewer, approved, current.instance().taskId(), true, ""))
                            .isInstanceOf(ApprovalException.class);
                    workflow.decide(reviewer, approved, next.instance().taskId(), true, "同意");
                    var completed = workflow.detail(applicant, approved);
                    assertThat(completed.instance().status()).isEqualTo(Status.APPROVED);
                    assertThat(completed.instance().definitionName()).isEqualTo("采购 V1");
                    assertThat(completed.actions()).extracting(Action::action)
                            .containsExactly(ActionType.START, ActionType.APPROVE, ActionType.APPROVE);

                    String rejected = workflow.start(applicant, form(second, "99"));
                    var task = workflow.detail(reviewer, rejected).instance();
                    assertThat(task.taskName()).isEqualTo("主管审批");
                    assertThatThrownBy(() -> workflow.decide(reviewer, rejected, task.taskId(), false, " "))
                            .isInstanceOf(ApprovalException.class);
                    workflow.decide(reviewer, rejected, task.taskId(), false, "缺少明细");
                    var rejectedDetail = workflow.detail(applicant, rejected);
                    assertThat(rejectedDetail.instance().status()).isEqualTo(Status.REJECTED);
                    assertThat(rejectedDetail.activities()).anyMatch(activity -> activity.id().equals("manager") && activity.cancelled());
                    assertThat(workflow.list(reviewer, Inbox.DONE, 0, 10).total()).isEqualTo(2);

                    String withdrawn = workflow.start(applicant, form(second, "99"));
                    assertThatThrownBy(() -> workflow.withdraw(reviewer, withdrawn, "")).isInstanceOf(ApprovalException.class);
                    workflow.withdraw(applicant, withdrawn, "撤回更正");
                    assertThat(workflow.detail(applicant, withdrawn).instance().status()).isEqualTo(Status.WITHDRAWN);
                    assertThat(workflow.list(reviewer, Inbox.TODO, 0, 10).total()).isZero();

                    assertThatThrownBy(() -> models.save(null, new SaveModel("invalid", Category.LEAVE, "",
                            BPMN.replace("<bpmn:userTask", "<bpmn:serviceTask"), fields, null, null)))
                            .isInstanceOf(ApprovalException.class);
                });
    }

    @Test
    void restoresVersionAsDraftAndPublishesItsRulesWithoutChangingInFlightInstances() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    var models = ctx.getBean(ApprovalModels.class);
                    var pages = ctx.getBean(ApprovalPages.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var engine = ctx.getBean(ProcessEngine.class);
                    var sql = new JdbcTemplate(ctx.getBean(DataSource.class));
                    var applicant = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("finance"));
                    var fields = List.of(new Field("amount", "采购金额", FieldType.NUMBER, true));
                    var page = pages.save(null, new SaveBusinessPage("PO", "采购详情", "flowable/order/approval.vue", "", true, 1));
                    String original = BPMN.replace("&gt;= 1000", "&gt; 2000");
                    var draft = models.save(null, new SaveModel("采购审批", Category.PURCHASE, "旧规则", original, fields, null, page.code()));
                    String modelId = draft.model().id();
                    assertThat(models.list(0, 10, "").list()).singleElement()
                            .satisfies(item -> assertThat(item.publishedVersion()).isNull());
                    var first = models.publish(modelId, 1);
                    assertThat(models.version(modelId, 1).xml()).isEqualTo(original);
                    String inFlight = workflow.start(applicant, form(first, "3280"));
                    assertThat(workflow.detail(reviewer, inFlight).instance().taskName()).isEqualTo("财务审批");
                    assertThat(workflow.detail(applicant, workflow.start(applicant, form(first, "2000", "PO-2"))).instance().taskName())
                            .isEqualTo("主管审批");

                    pages.save("PO", new SaveBusinessPage("PO", "采购详情新版", page.component(), "", true, 1));
                    String revised = original.replace("&gt; 2000", "&gt; 5000").replace("主管审批", "区域经理审批");
                    var updated = models.save(modelId, new SaveModel("采购审批二版", Category.OTHER, "新规则", revised, fields, 1, page.code()));
                    assertThat(models.definitions()).containsExactly(first);
                    assertThat(models.list(0, 10, "").list()).singleElement()
                            .satisfies(item -> assertThat(item.publishedVersion()).isEqualTo(1));
                    var second = models.publish(modelId, updated.model().revision());
                    assertThat(models.list(0, 10, "二版").list()).singleElement()
                            .satisfies(item -> assertThat(item.publishedVersion()).isEqualTo(2));
                    String newer = workflow.start(applicant, form(second, "3280", "PO-3"));
                    assertThat(workflow.detail(applicant, newer).instance().taskName()).isEqualTo("区域经理审批");
                    assertThat(models.versions(modelId, 0, 1).versions().total()).isEqualTo(2);
                    assertThat(models.versions(modelId, 0, 1).versions().list()).singleElement()
                            .satisfies(entry -> {
                                assertThat(entry.version()).isEqualTo(2);
                                assertThat(entry.current()).isTrue();
                                assertThat(entry.publishedAt()).isNotNull();
                            });
                    assertThat(models.versions(modelId, 1, 1).versions().list()).extracting(ModelVersion::version).containsExactly(1);
                    assertThatThrownBy(() -> models.restore(modelId, 1, 1)).isInstanceOf(ApprovalException.class)
                            .hasMessageContaining("草稿已更新");

                    // Older deployments may only contain the executable BPMN resource.
                    var process = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(first.id()).singleResult();
                    sql.update("DELETE FROM ACT_GE_BYTEARRAY WHERE DEPLOYMENT_ID_ = ? AND NAME_ = ?", process.getDeploymentId(),
                            "approval-source.xml");
                    var legacy = models.version(modelId, 1);
                    assertThat(legacy.xml()).isEqualTo(models.diagram(first.id()));
                    var restored = models.restore(modelId, 1, 2);
                    assertThat(restored.xml()).isEqualTo(legacy.xml());
                    assertThat(restored.fields()).isEqualTo(fields);
                    assertThat(restored.restoredPageConfig()).isEqualTo(page);
                    assertThat(restored.model().category()).isEqualTo(Category.PURCHASE);
                    assertThat(restored.model().description()).isEqualTo("旧规则");
                    assertThat(models.definitions()).containsExactly(second);
                    assertThat(models.versions(modelId, 0, 10).unpublishedChanges()).isTrue();
                    assertThat(models.list(0, 10, "").list()).singleElement()
                            .satisfies(item -> assertThat(item.publishedVersion()).isEqualTo(2));

                    var edited = models.save(modelId, new SaveModel(restored.model().name(), restored.model().category(),
                            restored.model().description(), restored.xml(), restored.fields(), restored.model().revision(), restored.businessPage()));
                    var third = models.publish(modelId, edited.model().revision());
                    assertThat(third.version()).isEqualTo(3);
                    assertThat(third.pageConfig()).isEqualTo(page);
                    assertThat(workflow.detail(applicant, workflow.start(applicant, form(third, "2000.01", "PO-4"))).instance().taskName())
                            .isEqualTo("财务审批");
                    var pending = workflow.detail(reviewer, inFlight).instance();
                    workflow.decide(reviewer, inFlight, pending.taskId(), true, "");
                    var next = workflow.detail(reviewer, inFlight).instance();
                    assertThat(next.definitionId()).isEqualTo(first.id());
                    assertThat(next.taskName()).isEqualTo("主管审批");
                    workflow.decide(reviewer, inFlight, next.taskId(), true, "");
                    assertThat(workflow.detail(applicant, inFlight).instance().status()).isEqualTo(Status.APPROVED);
                    assertThat(workflow.detail(applicant, newer).instance().definitionId()).isEqualTo(second.id());
                });
    }

    @Test
    void decimalBranchKeepsTheExactValueFromJson() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    var models = ctx.getBean(ApprovalModels.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var json = ctx.getBean(ObjectMapper.class);
                    var applicant = new Actor(1, "Applicant", List.of("root"));
                    var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                    var draft = models.save(null, new SaveModel("采购分支", Category.PURCHASE, "",
                            BPMN.replace("&gt;= 1000", "&gt; 100000000000.000001"), fields, null, null));
                    var definition = models.publish(draft.model().id(), 1);
                    String request = """
                            {"definitionId":"%s","requestId":"%s","title":"采购耗材","values":{"amount":%s}}
                            """;
                    var form = json.readValue(request.formatted(definition.id(), UUID.randomUUID(),
                            "100000000000.000002"), Start.class);
                    var above = workflow.detail(applicant, workflow.start(applicant, form));
                    assertThat(above.instance().taskName()).isEqualTo("财务审批");
                    assertThat(above.values().get("amount").decimalValue())
                            .isEqualByComparingTo("100000000000.000002");

                    var equalForm = json.readValue(request.formatted(definition.id(), UUID.randomUUID(),
                            "\"100000000000.000001\""), Start.class);
                    var equal = workflow.detail(applicant, workflow.start(applicant, equalForm));
                    assertThat(equal.instance().taskName()).isEqualTo("主管审批");
                    assertThat(equal.values().get("amount").textValue()).isEqualTo("100000000000.000001");
                });
    }

    @Test
    void linksTheDemoOrderToItsPublishedVersionAndKeepsLegacyDraftsReadable() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    var models = ctx.getBean(ApprovalModels.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var applicant = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("root", "finance"));
                    var fields = List.of(new Field("amount", "订单金额", FieldType.NUMBER, true));
                    String orderBpmn = BPMN.replace("&gt;= 1000", "&gt; 2000")
                            .replace("sourceRef=\"finance\" targetRef=\"manager\"", "sourceRef=\"finance\" targetRef=\"end\"");
                    var draft = models.save(null,
                            new SaveModel("订单审批", Category.PURCHASE, "", orderBpmn, fields, null, "DEMO_ORDER"));
                    assertThat(models.draft(draft.model().id()).businessPage()).isEqualTo("DEMO_ORDER");
                    var definition = models.publish(draft.model().id(), 1);
                    assertThat(models.definitions()).containsExactly(definition);
                    assertThat(definition.businessPage()).isEqualTo("DEMO_ORDER");
                    assertThatThrownBy(() -> workflow.start(applicant, form(definition, "999")))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("SO-DEMO-001");

                    var request = new Start(definition.id(), UUID.randomUUID().toString(), "测试订单审批", "SO-DEMO-001",
                            Map.of("amount", DecimalNode.valueOf(BigDecimal.ONE)));
                    String id = workflow.start(applicant, request);
                    var detail = workflow.detail(reviewer, id);
                    assertThat(detail.businessPage()).isEqualTo("DEMO_ORDER");
                    assertThat(detail.instance().taskName()).isEqualTo("财务审批");
                    assertThat(detail.values().get("amount").asText()).isEqualTo("3280.00");
                    assertThat(detail.values()).containsOnlyKeys("amount");
                    assertThat(workflow.list(applicant, Inbox.MINE, 0, 10, "SO-DEMO-001").list())
                            .extracting(Instance::id).containsExactly(id);
                    assertThat(workflow.list(reviewer, Inbox.MINE, 0, 10, "SO-DEMO-001").total()).isZero();
                    assertThat(workflow.list(applicant, Inbox.MINE, 0, 10, "SO-DEMO-002").total()).isZero();

                    var sql = new JdbcTemplate(ctx.getBean(DataSource.class));
                    sql.update("UPDATE flow_approval_instance SET values_json = ? WHERE id = ?",
                            "{\"amount\":\"3280.00\"}", id);
                    sql.update("UPDATE flow_approval_model SET fields_json = ? WHERE id = ?",
                            ctx.getBean(ObjectMapper.class).writeValueAsString(fields), draft.model().id());
                    assertThat(models.draft(draft.model().id()).businessPage()).isNull();
                    assertThat(models.draft(draft.model().id()).fields()).containsExactlyElementsOf(fields);

                    var updated = models.save(draft.model().id(),
                            new SaveModel("通用审批", Category.PURCHASE, "", orderBpmn, fields, 1, null));
                    var generic = models.publish(updated.model().id(), 2);
                    assertThat(generic.businessPage()).isNull();
                    assertThat(workflow.detail(reviewer, id).businessPage()).isEqualTo("DEMO_ORDER");
                    assertThatThrownBy(() -> workflow.start(applicant, new Start(generic.id(), UUID.randomUUID().toString(),
                            request.title(), request.businessNo(), request.values())))
                            .isInstanceOf(ApprovalException.class);

                    workflow.decide(reviewer, id, detail.instance().taskId(), true, "财务通过");
                    assertThat(workflow.detail(applicant, id).instance().status()).isEqualTo(Status.APPROVED);

                    assertThat(workflow.editableDefinition(applicant, id).id()).isEqualTo(definition.id());
                    String resubmitted = workflow.resubmit(applicant, id, request);
                    var repeated = workflow.detail(applicant, resubmitted);
                    assertThat(repeated.businessPage()).isEqualTo("DEMO_ORDER");
                    assertThat(repeated.instance().taskName()).isEqualTo("财务审批");
                    workflow.withdraw(applicant, resubmitted, "");

                    var order = new Order("新客户", "采购部", "2026-11-25", "2026-12-01", "联系人", "",
                            "新地址", "", List.of(new OrderItem("EQ-003", "键盘", 2, new BigDecimal("99.95"))));
                    workflow.saveOrder(applicant, order);
                    assertThat(workflow.order(applicant)).isEqualTo(order);
                    assertThat(workflow.order(reviewer).amount()).isEqualByComparingTo("3280.00");
                    var editedDefinition = models.publish(models.save(null,
                            new SaveModel("编辑订单审批", Category.PURCHASE, "", orderBpmn, fields, null, "DEMO_ORDER")).model().id(), 1);
                    var editedRequest = new Start(editedDefinition.id(), UUID.randomUUID().toString(), request.title(),
                            request.businessNo(), request.values());
                    var editedDetail = workflow.detail(reviewer, workflow.start(applicant, editedRequest));
                    assertThat(editedDetail.instance().taskName()).isEqualTo("主管审批");
                    assertThat(editedDetail.values().get("amount").asText()).isEqualTo("199.90");
                    assertThat(editedDetail.order()).isEqualTo(order);
                    assertThat(workflow.detail(reviewer, id).order().amount()).isEqualByComparingTo("3280.00");
                    var updatedOrder = new Order(order.customer(), order.department(), order.date(), order.deliveryDate(),
                            order.contact(), order.phone(), order.address(), "下次交付", order.items());
                    workflow.saveOrder(applicant, updatedOrder);
                    assertThat(workflow.order(applicant)).isEqualTo(updatedOrder);
                    assertThat(workflow.detail(reviewer, editedDetail.instance().id()).order()).isEqualTo(order);

                    String atThreshold = workflow.start(applicant, form(generic, "2000"));
                    assertThat(workflow.detail(applicant, atThreshold).instance().taskName()).isEqualTo("主管审批");
                    String aboveThreshold = workflow.start(applicant, form(generic, "2000.01"));
                    assertThat(workflow.detail(applicant, aboveThreshold).instance().taskName()).isEqualTo("财务审批");
                });
    }

    @Test
    void writesBackTheCurrentOrderApprovalAndBackfillsExistingInstances() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    var models = ctx.getBean(ApprovalModels.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var dataSource = ctx.getBean(DataSource.class);
                    var sql = new JdbcTemplate(dataSource);
                    sql.update("INSERT INTO sys_role VALUES (1, '管理员', 'root', 1, 0)");
                    sql.update("INSERT INTO sys_user_role VALUES (1, 1)");
                    var owner = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("finance"));
                    var fields = List.of(new Field("amount", "订单金额", FieldType.NUMBER, true));
                    var model = models.save(null, new SaveModel("订单审批", Category.PURCHASE, "",
                            BPMN, fields, null, DEMO_ORDER));
                    var definition = models.publish(model.model().id(), 1);
                    var draft = workflow.saveDraft(owner, null, orderForm(definition));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(null, null));

                    String older = workflow.resubmit(owner, draft, orderForm(definition));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(older, Status.PENDING));
                    var request = orderForm(definition);
                    assertThatThrownBy(() -> workflow.start(owner, request))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("已有审批中的申请");
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(older, Status.PENDING));
                    sql.update("DELETE FROM flow_approval_business_binding WHERE instance_id = ?", older);
                    assertThatThrownBy(() -> workflow.start(owner, request))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("已有审批中的申请");
                    workflow.withdraw(owner, older, "");
                    String current = workflow.start(owner, request);
                    assertThat(workflow.start(owner, request)).isEqualTo(current);
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(current, Status.PENDING));
                    assertThat(workflow.orderApproval(reviewer)).isEqualTo(new OrderApproval(null, null));

                    String finance = workflow.detail(reviewer, current).instance().taskId();
                    workflow.command(reviewer, current, new Command(Operation.TRANSFER, finance, owner.engineId(), null, ""));
                    workflow.command(owner, current, new Command(Operation.ASSIGN, finance, reviewer.engineId(), null, ""));
                    workflow.command(owner, current, new Command(Operation.CC, null, reviewer.engineId(), null, ""));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(current, Status.PENDING));
                    workflow.decide(reviewer, current, finance, true, "财务通过");
                    String manager = workflow.detail(reviewer, current).instance().taskId();
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(current, Status.PENDING));

                    assertThatThrownBy(() -> workflow.batch(reviewer, new BatchCommand(Operation.APPROVE,
                            List.of(new TaskRef(current, manager), new TaskRef("zzz-missing", "missing")), "", null)))
                            .isInstanceOf(ApprovalException.class);
                    assertThat(workflow.detail(reviewer, current).instance().taskId()).isEqualTo(manager);
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(current, Status.PENDING));
                    workflow.decide(reviewer, current, manager, true, "主管通过");
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(current, Status.APPROVED));

                    String rejected = workflow.resubmit(owner, current, orderForm(definition));
                    workflow.decide(reviewer, rejected, workflow.detail(reviewer, rejected).instance().taskId(), false, "驳回");
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(rejected, Status.REJECTED));
                    String editing = workflow.saveDraft(owner, rejected, orderForm(definition));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(rejected, Status.REJECTED));
                    String returned = workflow.resubmit(owner, editing, orderForm(definition));
                    workflow.command(reviewer, returned, new Command(Operation.RETURN,
                            workflow.detail(reviewer, returned).instance().taskId(), null, null, "修改订单"));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(returned, Status.RETURNED));
                    workflow.saveOrder(owner, new Order("已改客户", "采购部", "2026-11-25", null,
                            "", "", "", "", List.of(new OrderItem("SKU", "样品", 1, BigDecimal.TEN))));
                    String withdrawn = workflow.resubmit(owner, returned, orderForm(definition));
                    assertThat(workflow.detail(owner, withdrawn).sourceId()).isEqualTo(returned);
                    assertThat(workflow.detail(owner, withdrawn).order().customer()).isEqualTo("已改客户");
                    workflow.withdraw(owner, withdrawn, "");
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(withdrawn, Status.WITHDRAWN));
                    String cancelled = workflow.start(owner, orderForm(definition));
                    workflow.command(owner, cancelled, new Command(Operation.CANCEL, null, null, null, ""));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(cancelled, Status.CANCELLED));
                    String terminated = workflow.start(owner, orderForm(definition));
                    workflow.command(owner, terminated, new Command(Operation.TERMINATE,
                            workflow.detail(owner, terminated).instance().taskId(), null, null, "终止"));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(terminated, Status.TERMINATED));
                    String restarted = workflow.command(owner, terminated, new Command(Operation.RESTART, null, null, null, ""));
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(restarted, Status.PENDING));

                    workflow.saveDraft(owner, null, orderForm(definition));
                    sql.update("DELETE FROM flow_approval_order_status WHERE user_id = ?", owner.id());
                    migrateOrderApprovalStatus(dataSource);
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(restarted, Status.PENDING));
                    workflow.command(owner, restarted, new Command(Operation.FORCE_APPROVE,
                            workflow.detail(owner, restarted).instance().taskId(), null, null, "通过"));
                    migrateOrderApprovalStatus(dataSource);
                    assertThat(workflow.orderApproval(owner)).isEqualTo(new OrderApproval(restarted, Status.APPROVED));
                });
    }

    @Test
    void rejectsConcurrentSubmissionsForTheSameBusinessAcrossModels() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    var models = ctx.getBean(ApprovalModels.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var owner = new Actor(1, "Applicant", List.of("root"));
                    var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                    var first = models.publish(models.save(null, new SaveModel("订单流程一", Category.PURCHASE,
                            "", BPMN, fields, null, DEMO_ORDER)).model().id(), 1);
                    var second = models.publish(models.save(null, new SaveModel("订单流程二", Category.PURCHASE,
                            "", BPMN, fields, null, DEMO_ORDER)).model().id(), 1);
                    var ready = new CountDownLatch(2);
                    var results = List.of(first, second).stream().map(definition -> CompletableFuture.supplyAsync(() -> {
                        ready.countDown();
                        try {
                            if (!ready.await(5, TimeUnit.SECONDS)) {
                                throw new AssertionError("提交任务未就绪");
                            }
                            return workflow.start(owner, orderForm(definition));
                        } catch (ApprovalException exception) {
                            return exception.getMessage();
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(exception);
                        }
                    })).toList();
                    var outcomes = List.of(results.get(0).get(10, TimeUnit.SECONDS), results.get(1).get(10, TimeUnit.SECONDS));
                    var id = workflow.orderApproval(owner).instanceId();
                    assertThat(id).isNotNull();
                    assertThat(outcomes).containsExactlyInAnyOrder(id, "该业务单据已有审批中的申请");
                    assertThat(workflow.list(owner, Inbox.MINE, 0, 10, DEMO_ORDER_NO).list())
                            .extracting(Instance::id).containsExactly(id);
                });
    }

    @Test
    void deliversOperationCallbacksAndSkipsRetiredStatusCallbacks() throws Exception {
        var requests = new CopyOnWriteArrayList<String>();
        var rejectFirst = new AtomicBoolean(true);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/update", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestHeaders().getFirst("Idempotency-Key")
                    + " " + new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(rejectFirst.getAndSet(false) ? 503 : 204, -1);
            exchange.close();
        });
        server.start();
        try {
            context.withUserConfiguration(Database.class)
                    .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true",
                            "hpwit.flowable.callbacks.initial-delay-ms=60000",
                            "hpwit.flowable.callbacks.endpoints.receipt.business-page=WAREHOUSE_RECEIPT",
                            "hpwit.flowable.callbacks.endpoints.receipt.method=PATCH",
                            "hpwit.flowable.callbacks.endpoints.receipt.url=http://127.0.0.1:"
                                    + server.getAddress().getPort() + "/update",
                            "hpwit.flowable.callbacks.endpoints.receipt-action.business-page=WAREHOUSE_RECEIPT",
                            "hpwit.flowable.callbacks.endpoints.receipt-action.url=http://127.0.0.1:"
                                    + server.getAddress().getPort() + "/update")
                    .run(ctx -> {
                        assertThat(ctx).hasNotFailed();
                        var models = ctx.getBean(ApprovalModels.class);
                        var workflow = ctx.getBean(ApprovalWorkflow.class);
                        var callbacks = ctx.getBean(ApprovalCallbacks.class);
                        var json = ctx.getBean(ObjectMapper.class);
                        var sql = new JdbcTemplate(ctx.getBean(DataSource.class));
                        var applicant = new Actor(1, "Applicant", List.of("root"));
                        var reviewer = new Actor(2, "Reviewer", List.of("finance"));
                        var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                        ctx.getBean(ApprovalPages.class).save(null, new SaveBusinessPage("WAREHOUSE_RECEIPT",
                                "入库单", "flowable/order/approval.vue", "", true, 1));
                        assertThat(callbacks.options("WAREHOUSE_RECEIPT")).extracting(Option::value)
                                .containsExactly("receipt", "receipt-action");
                        assertThat(callbacks.options(DEMO_ORDER)).isEmpty();
                        var config = new StatusCallback(Map.of(),
                                Map.of(ActionType.COMMENT, "receipt", ActionType.RETURN, "receipt-action"));
                        assertThatThrownBy(() -> models.save(null, new SaveModel("不匹配业务页面", Category.OTHER,
                                "", BPMN, fields, null, DEMO_ORDER, config))).isInstanceOf(ApprovalException.class);
                        var draft = models.save(null, new SaveModel("入库审批", Category.OTHER, "", BPMN, fields, null,
                                "WAREHOUSE_RECEIPT"));
                        String modelId = draft.model().id();
                        var storedDraft = (ObjectNode) json.readTree(sql.queryForObject(
                                "SELECT fields_json FROM flow_approval_model WHERE id = ?", String.class, modelId));
                        var legacyConfig = storedDraft.putObject("statusCallback");
                        legacyConfig.put("endpoint", "retired-status");
                        legacyConfig.putObject("statusMapping").put("PENDING", 8).put("RETURNED", 4);
                        assertThat(json.convertValue(legacyConfig, StatusCallback.class)).isNull();
                        legacyConfig.putObject("operationEndpoints")
                                .put("COMMENT", "receipt").put("RETURN", "receipt-action");
                        sql.update("UPDATE flow_approval_model SET fields_json = ? WHERE id = ?", storedDraft.toString(), modelId);

                        assertThat(models.draft(modelId).statusCallback()).isEqualTo(config);
                        var first = models.publish(modelId, 1);
                        assertThat(first.statusCallback()).isEqualTo(config);
                        assertThat(json.valueToTree(first.statusCallback()).has("endpoint")).isFalse();
                        String id = workflow.start(applicant, form(first, "100"));
                        assertThat(workflow.detail(reviewer, id).callbackDelivery()).isNull();
                        assertThat(sql.queryForObject("SELECT COUNT(*) FROM flow_approval_status_callback", Integer.class))
                                .isZero();

                        var empty = models.save(modelId, new SaveModel("入库审批", Category.OTHER, "", BPMN, fields, 1,
                                "WAREHOUSE_RECEIPT"));
                        assertThat(models.publish(modelId, empty.model().revision()).statusCallback()).isNull();
                        assertThat(models.restore(modelId, 1, empty.model().revision()).statusCallback()).isEqualTo(config);

                        sql.update("""
                                INSERT INTO flow_approval_status_callback (endpoint, business_page, business_no,
                                    instance_id, applicant_id, approval_status, business_status)
                                VALUES ('receipt', 'WAREHOUSE_RECEIPT', 'PO-1', ?, 1, 'PENDING', 8)
                                """, id);
                        assertThat(workflow.detail(reviewer, id).callbackDelivery()).isNull();
                        String task = workflow.detail(reviewer, id).instance().taskId();
                        workflow.command(reviewer, id, new Command(Operation.COMMENT, null, null, null, "补充一"));
                        workflow.command(reviewer, id, new Command(Operation.COMMENT, null, null, null, "补充二"));
                        workflow.command(reviewer, id, new Command(Operation.RETURN, task, null, null, "补材料"));
                        assertThat(workflow.detail(reviewer, id).callbackDelivery().state()).isEqualTo("PENDING");
                        assertThat(requests).isEmpty();
                        callbacks.deliverPending();
                        assertThat(requests).hasSize(1);
                        assertThat(workflow.detail(reviewer, id).callbackDelivery().state()).isEqualTo("RETRYING");
                        callbacks.deliverPending();
                        assertThat(requests).hasSize(1);
                        sql.update("UPDATE flow_approval_status_callback SET next_attempt_at = CURRENT_TIMESTAMP(3)");
                        callbacks.deliverPending();
                        callbacks.deliverPending();
                        callbacks.deliverPending();
                        assertThat(requests).hasSize(4);
                        assertThat(requests.get(0)).startsWith("PATCH " + id + ":").isEqualTo(requests.get(1));
                        assertThat(requests.get(3)).startsWith("POST " + id + ":");
                        var comment = json.readTree(requests.get(1).substring(requests.get(1).indexOf('{')));
                        var secondComment = json.readTree(requests.get(2).substring(requests.get(2).indexOf('{')));
                        var returned = json.readTree(requests.get(3).substring(requests.get(3).indexOf('{')));
                        assertThat(comment.get("eventType").asText()).isEqualTo("OPERATION");
                        assertThat(comment.get("operation").asText()).isEqualTo("COMMENT");
                        assertThat(comment.get("approvalStatus").asText()).isEqualTo("PENDING");
                        assertThat(comment.has("status")).isFalse();
                        assertThat(comment.get("eventId")).isNotEqualTo(secondComment.get("eventId"));
                        assertThat(returned.get("operation").asText()).isEqualTo("RETURN");
                        assertThat(returned.get("approvalStatus").asText()).isEqualTo("RETURNED");
                        assertThat(returned.get("taskId").asText()).isEqualTo(task);
                        assertThat(sql.queryForObject("SELECT COUNT(*) FROM flow_approval_status_callback WHERE delivered_at IS NOT NULL",
                                Integer.class)).isEqualTo(3);
                        assertThat(workflow.detail(reviewer, id).callbackDelivery().state()).isEqualTo("DELIVERED");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void writesBackARegisteredBusinessPageAndRollsBackWhenItsHandlerFails() {
        context.withUserConfiguration(Database.class, WarehouseApprovalConfiguration.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    var models = ctx.getBean(ApprovalModels.class);
                    var pages = ctx.getBean(ApprovalPages.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var sql = new JdbcTemplate(ctx.getBean(DataSource.class));
                    var applicant = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("finance"));
                    sql.execute("""
                            CREATE TABLE warehouse_receipt (id BIGINT PRIMARY KEY, receipt_no VARCHAR(50) UNIQUE,
                                approval_status INT, approval_instance_id VARCHAR(64))
                            """);
                    sql.update("INSERT INTO warehouse_receipt (id, receipt_no) VALUES (42, 'PO-1')");

                    var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                    pages.save(null, new SaveBusinessPage("WAREHOUSE_RECEIPT", "入库单", "flowable/order/approval.vue", "", true, 1));
                    var mapping = new StatusCallback(Map.of(Status.PENDING, 1, Status.APPROVED, 2,
                            Status.REJECTED, 3, Status.RETURNED, 4, Status.WITHDRAWN, 5, Status.CANCELLED, 6, Status.TERMINATED, 7), Map.of());
                    var model = models.save(null, new SaveModel("入库单审批", Category.OTHER, "",
                            BPMN, fields, null, "WAREHOUSE_RECEIPT", mapping));
                    var definition = models.publish(model.model().id(), 1);
                    var generic = models.publish(models.save(null, new SaveModel("通用审批", Category.OTHER, "",
                            BPMN, fields, null, null)).model().id(), 1);
                    String genericId = workflow.start(applicant, form(generic, "100"));
                    assertThat(receiptStatus(sql)).isNull();

                    var request = form(definition, "100");
                    String id = workflow.start(applicant, request);
                    assertThat(receiptStatus(sql)).isEqualTo(1);
                    assertThat(sql.queryForObject("SELECT business_id FROM flow_approval_business_binding WHERE instance_id = ?",
                            String.class, id)).isEqualTo("42");
                    assertThat(workflow.orderApproval(applicant).status()).isNull();
                    String task = workflow.detail(reviewer, id).instance().taskId();
                    assertThatThrownBy(() -> workflow.decide(reviewer, id, task, false, "驳回"))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("入库单状态写入失败");
                    assertThat(workflow.detail(reviewer, id).instance().taskId()).isEqualTo(task);
                    assertThat(receiptStatus(sql)).isEqualTo(1);
                    sql.update("UPDATE warehouse_receipt SET receipt_no = 'RENAMED' WHERE id = 42");
                    assertThatThrownBy(() -> workflow.start(applicant, form(definition, "100", "RENAMED")))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("已有审批中的申请");
                    sql.update("UPDATE warehouse_receipt SET approval_instance_id = 'stale' WHERE id = 42");
                    assertThatThrownBy(() -> workflow.decide(reviewer, id, task, true, ""))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("入库单当前审批已变更");
                    assertThat(workflow.detail(reviewer, id).instance().status()).isEqualTo(Status.PENDING);
                    sql.update("UPDATE warehouse_receipt SET approval_instance_id = ? WHERE id = 42", id);
                    workflow.decide(reviewer, id, task, true, "");
                    assertThat(receiptStatus(sql)).isEqualTo(2);

                    workflow.withdraw(applicant, genericId, "");
                    assertThat(receiptStatus(sql)).isEqualTo(2);
                    var missing = new Start(definition.id(), UUID.randomUUID().toString(), request.title(),
                            "missing", request.values());
                    assertThatThrownBy(() -> workflow.start(applicant, missing))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("入库单不存在");
                    assertThat(workflow.list(applicant, Inbox.MINE, 0, 10, "missing").list()).isEmpty();
                    assertThat(ctx.getBean(ProcessEngine.class).getRuntimeService().createProcessInstanceQuery()
                            .processInstanceBusinessKey("missing").count()).isZero();
                });
    }

    @Test
    void administersBusinessPagesAndRunsWorkbenchActions() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    var models = ctx.getBean(ApprovalModels.class);
                    var pages = ctx.getBean(ApprovalPages.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var sql = new JdbcTemplate(ctx.getBean(DataSource.class));
                    sql.update("INSERT INTO sys_menu VALUES (11, 4, 'flowable:monitor')");
                    sql.update("INSERT INTO sys_role VALUES (3, '职员', 'employee', 1, 0), (4, '监控', 'monitor', 1, 0), (5, '管理员', 'root', 1, 0)");
                    sql.update("INSERT INTO sys_user_role VALUES (3, 3), (2, 4), (1, 5)");
                    sql.update("INSERT INTO sys_role_menu VALUES (3, 10), (4, 11)");
                    var admin = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("finance", "monitor"));
                    var third = new Actor(3, "Other", List.of("employee"));

                    pages.save(null, new SaveBusinessPage("PURCHASE", "采购详情", "flowable/order/approval.vue", "", true, 1));
                    assertThat(pages.enabled()).extracting(BusinessPage::code).containsExactly("DEMO_ORDER", "PURCHASE");
                    assertThat(pages.list(0, 10, "").total()).isEqualTo(2);
                    assertThat(pages.list(0, 10, " purchase ").list()).extracting(BusinessPage::code).containsExactly("PURCHASE");
                    assertThat(pages.list(0, 10, "采购").total()).isEqualTo(1);
                    assertThat(pages.list(0, 10, "%").list()).isEmpty();
                    var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                    var draft = models.save(null, new SaveModel("扩展审批", Category.PURCHASE, "Office stationery", BPMN, fields, null, "PURCHASE"));
                    assertThat(models.list(0, 10, "").total()).isEqualTo(1);
                    assertThat(models.list(0, 10, "扩展").list()).extracting(ModelSummary::id).containsExactly(draft.model().id());
                    assertThat(models.list(1, 10, "STATIONERY")).isEqualTo(new Page<ModelSummary>(List.of(), 1));
                    assertThat(models.list(0, 10, "不存在").total()).isZero();
                    var definition = models.publish(draft.model().id(), 1);
                    pages.save("PURCHASE", new SaveBusinessPage("PURCHASE", "采购详情改名", "flowable/order/approval.vue", "", false, 1));
                    assertThat(pages.enabled()).extracting(BusinessPage::code).containsExactly("DEMO_ORDER");
                    assertThat(models.definition(definition.id()).pageConfig().name()).isEqualTo("采购详情");
                    assertThatThrownBy(() -> models.save(null, new SaveModel("停用页面", Category.PURCHASE, "", BPMN, fields,
                            null, "PURCHASE"))).isInstanceOf(ApprovalException.class);

                    var request = form(definition, "1500");
                    var incomplete = new Start(request.definitionId(), request.requestId(), request.title(),
                            request.businessNo(), Map.of());
                    String draftId = workflow.saveDraft(admin, null, incomplete);
                    assertThat(workflow.saveDraft(admin, null, incomplete)).isEqualTo(draftId);
                    assertThatThrownBy(() -> workflow.start(admin, request)).isInstanceOf(ApprovalException.class)
                            .hasMessageContaining("已保存为草稿");
                    assertThat(workflow.detail(admin, draftId).instance().status()).isEqualTo(Status.DRAFT);
                    assertThatThrownBy(() -> workflow.detail(reviewer, draftId)).isInstanceOf(ApprovalException.class);
                    assertThat(workflow.editableDefinition(admin, draftId).id()).isEqualTo(definition.id());
                    String id = workflow.resubmit(admin, draftId, request);
                    assertThat(workflow.resubmit(admin, draftId, request)).isEqualTo(id);
                    assertThat(workflow.detail(admin, id).sourceId()).isEqualTo(draftId);
                    String task = workflow.detail(reviewer, id).instance().taskId();

                    assertThatThrownBy(() -> workflow.command(reviewer, id,
                            new Command(Operation.FORCE_APPROVE, task, null, null, "越权尝试")))
                            .isInstanceOf(ApprovalException.class);
                    workflow.command(admin, id, new Command(Operation.CC, null, "3", null, ""));
                    assertThat(workflow.list(third, Inbox.CC, 0, 10).list()).singleElement()
                            .satisfies(item -> assertThat(item.unread()).isEqualTo(1));
                    workflow.command(third, id, new Command(Operation.MARK_READ, null, null, null, ""));
                    assertThat(workflow.list(third, Inbox.CC, 0, 10).list()).singleElement()
                            .satisfies(item -> assertThat(item.unread()).isZero());
                    workflow.command(admin, id, new Command(Operation.CC, null, "2", null, ""));
                    workflow.command(admin, id, new Command(Operation.REMIND, null, null, null, ""));
                    assertThat(workflow.list(reviewer, Inbox.REMIND, 0, 10).total()).isEqualTo(1);
                    assertThat(workflow.markNoticesRead(reviewer, new ReadNotices(List.of(id)), NoticeKind.REMIND))
                            .isEqualTo(1);
                    assertThat(workflow.list(reviewer, Inbox.REMIND, 0, 10).list()).singleElement()
                            .satisfies(item -> assertThat(item.unread()).isZero());
                    assertThat(workflow.list(reviewer, Inbox.CC, 0, 10).list()).singleElement()
                            .satisfies(item -> assertThat(item.unread()).isEqualTo(1));
                    assertThatThrownBy(() -> workflow.command(admin, id, new Command(Operation.REMIND, null, null, null, "")))
                            .isInstanceOf(ApprovalException.class);
                    workflow.command(reviewer, id, new Command(Operation.COMMENT, null, null, null, "资料齐全"));
                    workflow.attach(reviewer, id, "订单.txt", new byte[]{1, 2, 3});
                    String attachmentId = workflow.detail(admin, id).attachments().get(0).id();
                    assertThat(workflow.attachment(third, id, attachmentId).base64()).isEqualTo("AQID");

                    workflow.command(reviewer, id, new Command(Operation.ADD_SIGN, task, "3", null, ""));
                    assertThat(workflow.list(third, Inbox.TODO, 0, 10).total()).isEqualTo(1);
                    workflow.decide(third, id, task, true, "加签通过");
                    assertThat(workflow.list(reviewer, Inbox.TODO, 0, 10).total()).isEqualTo(1);
                    workflow.command(reviewer, id, new Command(Operation.TRANSFER, task, "3", null, ""));
                    workflow.command(third, id, new Command(Operation.RETURN, task, null, null, "补充材料"));
                    assertThat(workflow.detail(admin, id).instance().status()).isEqualTo(Status.RETURNED);
                    assertThatThrownBy(() -> workflow.command(admin, id, new Command(Operation.DELETE, null, null, null, "")))
                            .isInstanceOf(ApprovalException.class);
                    String edit = workflow.saveDraft(admin, id, form(definition, "1800"));
                    String next = workflow.resubmit(admin, edit, form(definition, "1800"));
                    assertThat(workflow.detail(admin, next).sourceId()).isEqualTo(id);

                    String nextTask = workflow.detail(admin, next).instance().taskId();
                    workflow.command(admin, next, new Command(Operation.MOVE, nextTask, null, "manager", "演示干预"));
                    var moved = workflow.detail(admin, next);
                    assertThat(moved.instance().taskName()).isEqualTo("主管审批");
                    assertThat(moved.instance().taskId()).isNotEqualTo(nextTask);
                    workflow.command(admin, next, new Command(Operation.ASSIGN, moved.instance().taskId(), "3", null, ""));
                    workflow.command(admin, next, new Command(Operation.TERMINATE, moved.instance().taskId(), null, null, "测试终止"));
                    assertThat(workflow.list(admin, Inbox.ENDED, 0, 10).list()).anyMatch(item -> item.id().equals(next));
                    String restarted = workflow.command(admin, next, new Command(Operation.RESTART, null, null, null, ""));
                    var restartedTask = workflow.detail(admin, restarted).instance().taskId();
                    workflow.command(admin, restarted, new Command(Operation.FORCE_APPROVE, restartedTask, null, null, "强制通过测试"));
                    assertThat(workflow.detail(admin, restarted).instance().status()).isEqualTo(Status.APPROVED);

                    String batchFirst = workflow.start(admin, form(definition, "99"));
                    String batchSecond = workflow.start(admin, form(definition, "99", "PO-10"));
                    var batchTodo = workflow.list(reviewer, Inbox.TODO, 0, 10,
                            null, new InboxFilter("PO-1", Category.PURCHASE, Status.PENDING, null, null, null));
                    var batchItems = batchTodo.list().stream()
                            .filter(item -> List.of(batchFirst, batchSecond).contains(item.id()))
                            .map(item -> new TaskRef(item.id(), item.taskId()))
                            .sorted(java.util.Comparator.comparing(TaskRef::instanceId)).toList();
                    assertThat(batchItems).hasSize(2);
                    assertThatThrownBy(() -> workflow.batch(reviewer,
                            new BatchCommand(Operation.APPROVE, List.of(batchItems.get(0),
                                    new TaskRef(batchItems.get(1).instanceId(), "stale")), null, "批量通过")))
                            .isInstanceOf(ApprovalException.class);
                    assertThat(workflow.detail(admin, batchItems.get(0).instanceId()).instance().status()).isEqualTo(Status.PENDING);
                    workflow.batch(reviewer, new BatchCommand(Operation.APPROVE, batchItems, null, "批量通过"));
                    assertThat(workflow.list(reviewer, Inbox.DONE, 0, 10).list())
                            .anyMatch(item -> item.id().equals(batchFirst) && "批量通过".equals(item.activityComment()));

                    String cancelled = workflow.start(admin, form(definition, "99"));
                    workflow.command(admin, cancelled, new Command(Operation.CANCEL, null, null, null, ""));
                    assertThat(workflow.detail(admin, cancelled).instance().status()).isEqualTo(Status.CANCELLED);
                    String deleted = workflow.saveDraft(admin, null, form(definition, "99"));
                    workflow.command(admin, deleted, new Command(Operation.DELETE, null, null, null, ""));
                    assertThatThrownBy(() -> workflow.detail(admin, deleted)).isInstanceOf(ApprovalException.class);
                });
    }

    @Test
    void blocksUnavailableApproversAtPublishAndRollsBackBlockedTransitions() {
        context.withUserConfiguration(Database.class)
                .withPropertyValues("hpwit.flowable.enabled=true", "hpwit.flowable.schema-update=true")
                .run(ctx -> {
                    var models = ctx.getBean(ApprovalModels.class);
                    var workflow = ctx.getBean(ApprovalWorkflow.class);
                    var sql = new JdbcTemplate(ctx.getBean(DataSource.class));
                    var applicant = new Actor(1, "Applicant", List.of("root"));
                    var reviewer = new Actor(2, "Reviewer", List.of("finance"));
                    var fields = List.of(new Field("amount", "金额", FieldType.NUMBER, true));
                    sql.update("INSERT INTO sys_role VALUES (3, '备用', 'backup', 1, 0)");
                    sql.update("INSERT INTO sys_user_role VALUES (3, 3)");
                    var draft = models.save(null, new SaveModel("采购审批", Category.PURCHASE, "",
                            BPMN.replace("flowable:assignee=\"2\"", "flowable:assignee=\"3\""), fields, null, null));
                    assertThat(models.approvers(false, "")).extracting(Option::value).containsExactly("2");
                    assertThat(models.approvers(true, "")).extracting(Option::value).containsExactly("finance");
                    assertThatThrownBy(() -> models.publish(draft.model().id(), 1))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("主管审批");
                    sql.update("INSERT INTO sys_role_menu VALUES (3, 10)");
                    var definition = models.publish(draft.model().id(), 1);
                    String id = workflow.start(applicant, form(definition, "1500"));
                    String task = workflow.detail(reviewer, id).instance().taskId();

                    sql.update("DELETE FROM sys_role_menu WHERE role_id = 3");
                    assertThatThrownBy(() -> workflow.decide(reviewer, id, task, true, ""))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("主管审批");
                    assertThat(workflow.detail(reviewer, id).instance().taskId()).isEqualTo(task);
                    assertThatThrownBy(() -> workflow.start(applicant, form(definition, "100")))
                            .isInstanceOf(ApprovalException.class).hasMessageContaining("主管审批");
                    assertThat(workflow.list(applicant, Inbox.MINE, 0, 10).total()).isEqualTo(1);
                });
    }

    private Start form(Definition definition, String value) {
        return form(definition, value, "PO-1");
    }

    private Start form(Definition definition, String value, String businessNo) {
        return new Start(definition.id(), UUID.randomUUID().toString(), "采购耗材", businessNo,
                Map.of("amount", DecimalNode.valueOf(new BigDecimal(value))));
    }

    private Start orderForm(Definition definition) {
        return new Start(definition.id(), UUID.randomUUID().toString(), "测试订单审批", DEMO_ORDER_NO, Map.of());
    }

    private Integer receiptStatus(JdbcTemplate sql) {
        return sql.queryForObject("SELECT approval_status FROM warehouse_receipt WHERE id = 42", Integer.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class WarehouseApprovalConfiguration {
        @Bean
        BusinessApprovalStatusHandler warehouseApprovalStatus(JdbcClient jdbc) {
            return new BusinessApprovalStatusHandler() {
                @Override
                public String businessPage() {
                    return "WAREHOUSE_RECEIPT";
                }

                @Override
                public String resolveBusinessId(String businessNo, long applicantId) {
                    if (applicantId != 1) {
                        throw new ApprovalException("无权提交此入库单");
                    }
                    return jdbc.sql("SELECT id FROM warehouse_receipt WHERE receipt_no = ?").param(businessNo)
                            .query(String.class).optional().orElseThrow(() -> new ApprovalException("入库单不存在"));
                }

                @Override
                public boolean supportsStatusMapping() {
                    return true;
                }

                @Override
                public void updateStatus(ApprovalReference approval, Status status) {
                    long id = Long.parseLong(approval.businessId());
                    int updated = status == Status.PENDING
                            ? jdbc.sql("UPDATE warehouse_receipt SET approval_status = ?, approval_instance_id = ? WHERE id = ?")
                                    .params(approval.businessStatus(), approval.instanceId(), id).update()
                            : jdbc.sql("UPDATE warehouse_receipt SET approval_status = ? WHERE id = ? AND approval_instance_id = ?")
                                    .params(approval.businessStatus(), id, approval.instanceId()).update();
                    if (updated == 0) {
                        throw new ApprovalException("入库单当前审批已变更");
                    }
                    if (status == Status.REJECTED) {
                        throw new ApprovalException("入库单状态写入失败");
                    }
                }
            };
        }
    }

    private void migrateOrderApprovalStatus(DataSource dataSource) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("SET MODE MYSQL");
            try {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/flowable/upgrade-order-approval-status.sql"));
            } finally {
                statement.execute("SET MODE LEGACY");
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class Database {
        @Bean
        DataSource dataSource() throws SQLException {
            var db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .setName("flow-approval;MODE=LEGACY").build();
            try (var connection = db.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SET MODE MYSQL");
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/flowable/schema.sql"));
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/flowable/upgrade-workbench.sql"));
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/flowable/upgrade-order-approval-status.sql"));
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/flowable/upgrade-status-callback.sql"));
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/flowable/upgrade-business-approval-binding.sql"));
                statement.execute("SET MODE LEGACY");
            }
            var sql = new JdbcTemplate(db);
            sql.execute("CREATE TABLE sys_user (id BIGINT, username VARCHAR(64), nickname VARCHAR(64), status INT, is_deleted INT)");
            sql.execute("CREATE TABLE sys_role (id BIGINT, name VARCHAR(64), code VARCHAR(64), status INT, is_deleted INT)");
            sql.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
            sql.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
            sql.execute("CREATE TABLE sys_menu (id BIGINT, type INT, perm VARCHAR(64))");
            sql.update("INSERT INTO sys_user VALUES (1, 'applicant', NULL, 1, 0), (2, 'reviewer', NULL, 1, 0), (3, 'other', NULL, 1, 0)");
            sql.update("INSERT INTO sys_role VALUES (2, '财务', 'finance', 1, 0)");
            sql.update("INSERT INTO sys_user_role VALUES (2, 2)");
            sql.update("INSERT INTO sys_menu VALUES (10, 4, 'flowable:work')");
            sql.update("INSERT INTO sys_role_menu VALUES (2, 10)");
            return db;
        }

        @Bean
        JdbcTransactionManager transactionManager(DataSource datasource) {
            return new JdbcTransactionManager(datasource);
        }

        @Bean
        JdbcClient jdbcClient(DataSource datasource) {
            return JdbcClient.create(datasource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BPMN = """
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                xmlns:flowable="http://flowable.org/bpmn" targetNamespace="https://example.org/approval">
              <bpmn:process id="Process_1">
                <bpmn:startEvent id="start"/>
                <bpmn:exclusiveGateway id="choice" default="normal"/>
                <bpmn:userTask id="finance" name="财务审批" flowable:candidateGroups="finance"/>
                <bpmn:userTask id="manager" name="主管审批" flowable:assignee="2"/>
                <bpmn:endEvent id="end"/>
                <bpmn:sequenceFlow id="begin" sourceRef="start" targetRef="choice"/>
                <bpmn:sequenceFlow id="high" sourceRef="choice" targetRef="finance">
                  <bpmn:conditionExpression>${amount &gt;= 1000}</bpmn:conditionExpression>
                </bpmn:sequenceFlow>
                <bpmn:sequenceFlow id="normal" sourceRef="choice" targetRef="manager"/>
                <bpmn:sequenceFlow id="toManager" sourceRef="finance" targetRef="manager"/>
                <bpmn:sequenceFlow id="complete" sourceRef="manager" targetRef="end"/>
              </bpmn:process>
            </bpmn:definitions>
            """;
}
