package com.hpwit.modules.flowable.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpwit.modules.flowable.model.ApprovalData.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER;
import static com.hpwit.modules.flowable.service.ApprovalBpmn.require;

@Service
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
public class ApprovalModels {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalModels.class);
    private static final String DEPLOYMENT_CATEGORY = "hpwit-approval";
    private static final String FORM_RESOURCE = "approval-form.json";
    private static final String SOURCE_RESOURCE = "approval-source.xml";
    private static final Set<String> RESERVED_KEYS = Set.of("applicantid", "execution", "task", "authenticateduserid",
            "true", "false", "null", "empty", "div", "mod", "and", "or", "not", "eq", "ne", "lt", "le", "gt", "ge",
            "instanceof", "new");

    private record DraftForm(List<Field> fields, String businessPage, BusinessPage restoredPageConfig, StatusCallback statusCallback) {
    }

    private record PublishedForm(String modelId, String description, Category category, List<Field> fields,
                                 String businessPage, BusinessPage pageConfig, StatusCallback statusCallback) {
    }

    private final ApprovalStore store;
    private final RepositoryService repository;
    private final ObjectMapper json;
    private final ApprovalPages pages;
    private final ApprovalCallbacks callbacks;

    public ApprovalModels(ApprovalStore store, ProcessEngine engine, ObjectMapper json, ApprovalPages pages,
                          ApprovalCallbacks callbacks) {
        this.store = store;
        this.repository = engine.getRepositoryService();
        this.json = json;
        this.pages = pages;
        this.callbacks = callbacks;
    }

    public Page<ModelSummary> list(int offset, int size, String keyword) {
        var page = store.models(offset, size, keyword);
        var ids = page.list().stream().map(ModelSummary::definitionId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return page;
        }
        var versions = repository.createProcessDefinitionQuery().processDefinitionIds(ids).list().stream()
                .collect(Collectors.toMap(ProcessDefinition::getId, ProcessDefinition::getVersion));
        var models = page.list().stream().map(model -> model.withPublishedVersion(versions.get(model.definitionId()))).toList();
        return new Page<>(models, page.total());
    }

    public Draft draft(String id) {
        return toDraft(model(id, false));
    }

    @Transactional
    public Draft save(String id, SaveModel form) {
        var keys = new HashSet<String>();
        for (Field field : form.fields()) {
            String key = field.key().toLowerCase(Locale.ROOT);
            require(!RESERVED_KEYS.contains(key) && keys.add(key), "表单字段编码不可重复或使用系统保留名称");
        }
        ApprovalBpmn.checkDraft(form.xml());
        if (DEMO_ORDER.equals(form.businessPage())) {
            require(form.fields().stream().noneMatch(field -> "amount".equals(field.key()) && field.type() != FieldType.NUMBER),
                    "测试订单的 amount 字段必须为数字类型");
        }
        BusinessPage restoredPage = null;
        if (id != null) {
            var current = toDraft(model(id, true));
            require(form.revision() != null && form.revision() == current.model().revision(), "草稿已更新，请重新打开后编辑");
            if (Objects.equals(form.businessPage(), current.businessPage())) {
                restoredPage = current.restoredPageConfig();
            }
        }
        if (restoredPage == null) {
            pages.active(form.businessPage());
        }
        callbacks.validate(form.businessPage(), form.statusCallback());
        String fields = writeJson(new DraftForm(form.fields(), form.businessPage(), restoredPage, form.statusCallback()));
        if (id == null) {
            id = UUID.randomUUID().toString();
            store.insertModel(id, "approval_" + id.replace("-", ""), form, fields);
        } else {
            store.updateModel(id, form, fields);
        }
        return draft(id);
    }

    @Transactional
    public Definition publish(String id, int revision) {
        var draft = toDraft(model(id, true));
        var model = draft.model();
        require(model.revision() == revision, "草稿已更新，请重新打开后发布");
        if (model.publishedRevision() != null && model.publishedRevision() == revision) {
            return definition(model.definitionId());
        }

        String xml = ApprovalBpmn.prepare(draft.xml(), model.processKey(), model.name(), draft.fields(),
                this::approvalUser, store::approvalRole);
        var page = draft.restoredPageConfig() == null ? pages.active(draft.businessPage()) : draft.restoredPageConfig();
        callbacks.validate(draft.businessPage(), draft.statusCallback());
        String metadata = writeJson(new PublishedForm(id, model.description(), model.category(), draft.fields(),
                draft.businessPage(), page, draft.statusCallback()));
        try {
            var deployment = repository.createDeployment()
                    .key(id)
                    .name(model.name())
                    .category(DEPLOYMENT_CATEGORY)
                    .addString(model.processKey() + ".bpmn20.xml", xml)
                    .addString(FORM_RESOURCE, metadata)
                    .addString(SOURCE_RESOURCE, draft.xml())
                    .deploy();
            var process = repository.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            store.publishModel(id, process.getId());
            return toDefinition(process);
        } catch (FlowableException e) {
            LOGGER.warn("Approval model deployment failed for {}", id, e);
            throw new ApprovalException("Flowable 校验或部署失败，请检查 BPMN 模型");
        }
    }

    @Transactional(readOnly = true)
    public ModelVersions versions(String id, int offset, int size) {
        var model = model(id, false).summary();
        var query = repository.createProcessDefinitionQuery().processDefinitionKey(model.processKey());
        long total = query.count();
        var definitions = query.orderByProcessDefinitionVersion().desc().listPage(offset, size);
        boolean unpublishedChanges = !Objects.equals(model.publishedRevision(), model.revision());
        if (definitions.isEmpty()) {
            return new ModelVersions(model.revision(), unpublishedChanges, new Page<>(List.of(), total));
        }
        var deployments = repository.createDeploymentQuery()
                .deploymentIds(definitions.stream().map(ProcessDefinition::getDeploymentId).distinct().toList())
                .list().stream().collect(Collectors.toMap(Deployment::getId, Function.identity()));
        var versions = definitions.stream().map(definition -> new ModelVersion(definition.getVersion(), definition.getName(),
                deployments.get(definition.getDeploymentId()).getDeploymentTime().toInstant(),
                definition.getId().equals(model.definitionId()))).toList();
        return new ModelVersions(model.revision(), unpublishedChanges, new Page<>(versions, total));
    }

    @Transactional(readOnly = true)
    public ModelVersionDetail version(String id, int version) {
        var process = versionProcess(model(id, false).summary(), version);
        return new ModelVersionDetail(toDefinition(process), source(process));
    }

    @Transactional
    public Draft restore(String id, int version, int revision) {
        var model = model(id, true).summary();
        require(revision == model.revision(), "草稿已更新，请刷新版本历史后重试");
        var process = versionProcess(model, version);
        var published = publishedForm(process);
        var form = new SaveModel(process.getName(), published.category(), published.description(), source(process),
                published.fields(), revision, published.businessPage(), published.statusCallback());
        store.updateModel(id, form, writeJson(new DraftForm(form.fields(), form.businessPage(),
                publishedPage(published), form.statusCallback())));
        return draft(id);
    }

    public List<Definition> definitions() {
        return store.publishedDefinitionIds().stream().map(this::definition).toList();
    }

    public Definition definition(String id) {
        return toDefinition(process(id));
    }

    Definition resubmissionDefinition(String id, boolean lock) {
        var original = definition(id);
        var latestId = model(original.modelId(), lock).summary().definitionId();
        if (id.equals(latestId)) {
            return original;
        }
        var latest = definition(latestId);
        return Objects.equals(original.businessPage(), latest.businessPage()) ? latest : original;
    }

    public String diagram(String id) {
        var process = process(id);
        return resource(process.getDeploymentId(), process.getResourceName());
    }

    public List<Option> approvers(boolean roles, String search) {
        return roles ? store.roleOptions(search) : store.userOptions(search);
    }

    ApprovalStore.ModelRow model(String id, boolean lock) {
        return store.model(id, lock).orElseThrow(() -> new ApprovalException("审批模型不存在"));
    }

    private ProcessDefinition process(String id) {
        var process = repository.createProcessDefinitionQuery().processDefinitionId(id).singleResult();
        require(process != null && process.getKey().startsWith("approval_"), "流程版本不存在");
        return process;
    }

    private ProcessDefinition versionProcess(ModelSummary model, int version) {
        var process = repository.createProcessDefinitionQuery().processDefinitionKey(model.processKey())
                .processDefinitionVersion(version).singleResult();
        require(process != null, "此模型没有该发布版本");
        return process;
    }

    private String source(ProcessDefinition process) {
        var deployment = process.getDeploymentId();
        String name = repository.getDeploymentResourceNames(deployment).contains(SOURCE_RESOURCE)
                ? SOURCE_RESOURCE : process.getResourceName();
        return resource(deployment, name);
    }

    private PublishedForm publishedForm(ProcessDefinition process) {
        return readJson(resource(process.getDeploymentId(), FORM_RESOURCE), new TypeReference<>() {});
    }

    private Definition toDefinition(ProcessDefinition process) {
        PublishedForm form = publishedForm(process);
        return new Definition(process.getId(), form.modelId(), process.getKey(), process.getName(), form.category(),
                form.description(), process.getVersion(), form.fields(), form.businessPage(),
                publishedPage(form), form.statusCallback());
    }

    private BusinessPage publishedPage(PublishedForm form) {
        return form.pageConfig() == null ? pages.legacy(form.businessPage()) : form.pageConfig();
    }

    private Draft toDraft(ApprovalStore.ModelRow row) {
        JsonNode stored = readJson(row.fields(), new TypeReference<>() {});
        DraftForm form = stored.isArray()
                ? new DraftForm(json.convertValue(stored, new TypeReference<>() {}), null, null, null)
                : json.convertValue(stored, DraftForm.class);
        return new Draft(row.summary(), row.xml(), form.fields(), form.businessPage(), form.restoredPageConfig(),
                form.statusCallback());
    }

    private boolean approvalUser(String id) {
        try {
            return store.approvalUser(Long.parseLong(id)).isPresent();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String resource(String deploymentId, String name) {
        try (var input = repository.getResourceAsStream(deploymentId, name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取流程版本文件失败", e);
        }
    }

    String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("保存审批表单失败", e);
        }
    }

    <T> T readJson(String value, TypeReference<T> type) {
        try {
            return json.readerFor(type).with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readValue(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("读取审批表单失败", e);
        }
    }
}
