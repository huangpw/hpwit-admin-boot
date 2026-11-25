package com.hpwit.modules.flowable.controller;

import com.hpwit.common.result.Result;
import com.hpwit.modules.flowable.model.ApprovalData.*;
import com.hpwit.modules.flowable.service.ApprovalAccess;
import com.hpwit.modules.flowable.service.ApprovalCallbacks;
import com.hpwit.modules.flowable.service.ApprovalException;
import com.hpwit.modules.flowable.service.ApprovalModels;
import com.hpwit.modules.flowable.service.ApprovalPages;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import com.hpwit.modules.flowable.service.ApprovalWorkflow;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.hpwit.modules.flowable.service.ApprovalAccess.*;

@Tag(name = "流程审批")
@RestController
@RequestMapping("/v1/flowable")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
public class ApprovalController {

    private final ApprovalAccess access;
    private final ApprovalModels models;
    private final ApprovalWorkflow workflow;
    private final ApprovalPages pages;
    private final ApprovalCallbacks callbacks;

    @GetMapping("/business-pages")
    public Result<Page<BusinessPage>> businessPages(
            @RequestParam(defaultValue = "1") @Min(1) @Max(1000000) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(defaultValue = "") @Size(max = 150) String keyword) {
        access.current(PAGES);
        return Result.success(pages.list((pageNum - 1) * pageSize, pageSize, keyword));
    }

    @GetMapping("/business-pages/options")
    public Result<List<BusinessPage>> businessPageOptions() {
        access.current(MODEL);
        return Result.success(pages.enabled());
    }

    @GetMapping("/models/status-callback-options")
    public Result<List<Option>> statusCallbackOptions(@RequestParam @Size(max = 64) String businessPage) {
        access.current(MODEL);
        return Result.success(callbacks.options(businessPage));
    }

    @PostMapping("/business-pages")
    public Result<BusinessPage> createBusinessPage(@Valid @RequestBody SaveBusinessPage form) {
        access.current(PAGES);
        return Result.success(pages.save(null, form));
    }

    @PutMapping("/business-pages/{code}")
    public Result<BusinessPage> updateBusinessPage(@PathVariable String code, @Valid @RequestBody SaveBusinessPage form) {
        access.current(PAGES);
        return Result.success(pages.save(code, form));
    }

    @GetMapping("/participants")
    public Result<List<Option>> participants(@RequestParam(defaultValue = "") @Size(max = 100) String search) {
        var actor = access.current();
        if (!access.can(actor, ADMIN) && !access.can(actor, WORK)) {
            throw new ApprovalException("当前用户没有审批模块的相应权限");
        }
        return Result.success(workflow.recipients(search));
    }

    @GetMapping("/models")
    public Result<Page<ModelSummary>> models(@RequestParam(defaultValue = "1") @Min(1) @Max(1000000) int pageNum,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                                              @RequestParam(defaultValue = "") @Size(max = 150) String keyword) {
        access.current(MODEL);
        return Result.success(models.list((pageNum - 1) * pageSize, pageSize, keyword));
    }

    @GetMapping("/models/{id}")
    public Result<Draft> draft(@PathVariable String id) {
        access.current(MODEL);
        return Result.success(models.draft(id));
    }

    @PostMapping("/models")
    public Result<Draft> create(@Valid @RequestBody SaveModel form) {
        access.current(MODEL);
        return Result.success(models.save(null, form));
    }

    @PutMapping("/models/{id}")
    public Result<Draft> update(@PathVariable String id, @Valid @RequestBody SaveModel form) {
        access.current(MODEL);
        return Result.success(models.save(id, form));
    }

    @PostMapping("/models/{id}/publish")
    public Result<Definition> publish(@PathVariable String id, @RequestParam @Positive int revision) {
        access.current(MODEL);
        return Result.success(models.publish(id, revision));
    }

    @GetMapping("/models/{id}/versions")
    public Result<ModelVersions> modelVersions(@PathVariable String id,
                                              @RequestParam(defaultValue = "1") @Min(1) @Max(1000000) int pageNum,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        access.current(MODEL);
        return Result.success(models.versions(id, (pageNum - 1) * pageSize, pageSize));
    }

    @GetMapping("/models/{id}/versions/{version}")
    public Result<ModelVersionDetail> modelVersion(@PathVariable String id, @PathVariable @Positive int version) {
        access.current(MODEL);
        return Result.success(models.version(id, version));
    }

    @PostMapping("/models/{id}/versions/{version}/restore")
    public Result<Draft> restoreModelVersion(@PathVariable String id, @PathVariable @Positive int version,
                                             @Valid @RequestBody RestoreModelVersion form) {
        access.current(MODEL);
        return Result.success(models.restore(id, version, form.revision()));
    }

    @GetMapping("/approvers")
    public Result<List<Option>> approvers(@RequestParam(defaultValue = "false") boolean roles,
                                          @RequestParam(defaultValue = "") @Size(max = 100) String search) {
        access.current(MODEL);
        return Result.success(models.approvers(roles, search));
    }

    @GetMapping("/definitions")
    public Result<List<Definition>> definitions() {
        access.current(WORK);
        return Result.success(models.definitions());
    }

    @PostMapping("/instances")
    public Result<String> start(@Valid @RequestBody Start form) {
        return Result.success(workflow.start(access.current(WORK), form));
    }

    @GetMapping("/order")
    public Result<Order> order() {
        return Result.success(workflow.order(access.current(WORK)));
    }

    @GetMapping("/order/approval-status")
    public Result<OrderApproval> orderApproval() {
        return Result.success(workflow.orderApproval(access.current(WORK)));
    }

    @PutMapping("/order")
    public Result<Order> saveOrder(@Valid @RequestBody Order form) {
        return Result.success(workflow.saveOrder(access.current(WORK), form));
    }

    @GetMapping("/overview")
    public Result<Overview> overview() {
        return Result.success(workflow.overview(access.current(WORK)));
    }

    @PostMapping("/instances/batch")
    public Result<Void> batch(@Valid @RequestBody BatchCommand form) {
        workflow.batch(access.current(WORK), form);
        return Result.success();
    }

    @PostMapping({"/copies/read", "/notices/read"})
    public Result<Integer> markNoticesRead(@Valid @RequestBody ReadNotices form,
                                          @RequestParam(defaultValue = "CC") NoticeKind kind) {
        return Result.success(workflow.markNoticesRead(access.current(WORK), form, kind));
    }

    @GetMapping("/instances")
    public Result<Page<Instance>> instances(@RequestParam(defaultValue = "MINE") Inbox inbox,
                                            @RequestParam(defaultValue = "1") @Min(1) @Max(1000000) int pageNum,
                                            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                                            @RequestParam(required = false) @Size(max = 100) String businessNo,
                                            @RequestParam(required = false) @Size(max = 150) String keyword,
                                            @RequestParam(required = false) Category category,
                                            @RequestParam(required = false) Status status,
                                            @RequestParam(required = false) Instant from,
                                            @RequestParam(required = false) Instant until,
                                            @RequestParam(required = false) Boolean unread) {
        var actor = inbox == Inbox.ALL || inbox == Inbox.ENDED ? access.current() : access.current(WORK);
        var filter = new InboxFilter(keyword, category, status, from, until, unread);
        return Result.success(workflow.list(actor, inbox, (pageNum - 1) * pageSize, pageSize, businessNo, filter));
    }

    @GetMapping("/instances/{id}")
    public Result<Detail> detail(@PathVariable String id) {
        return Result.success(workflow.detail(access.current(), id));
    }

    @PostMapping("/instances/{id}/tasks/{taskId}/approve")
    public Result<Void> approve(@PathVariable String id, @PathVariable String taskId, @Valid @RequestBody Comment form) {
        workflow.decide(access.current(WORK), id, taskId, true, form.comment());
        return Result.success();
    }

    @PostMapping("/instances/{id}/tasks/{taskId}/reject")
    public Result<Void> reject(@PathVariable String id, @PathVariable String taskId, @Valid @RequestBody Comment form) {
        workflow.decide(access.current(WORK), id, taskId, false, form.comment());
        return Result.success();
    }

    @PostMapping("/instances/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable String id, @Valid @RequestBody Comment form) {
        workflow.withdraw(access.current(WORK), id, form.comment());
        return Result.success();
    }

    @PostMapping("/instances/{id}/actions")
    public Result<String> command(@PathVariable String id, @Valid @RequestBody Command form) {
        return Result.success(workflow.command(access.current(), id, form));
    }

    @PostMapping("/drafts")
    public Result<String> createDraft(@Valid @RequestBody Start form) {
        return Result.success(workflow.saveDraft(access.current(WORK), null, form));
    }

    @PutMapping("/drafts/{id}")
    public Result<String> saveDraft(@PathVariable String id, @Valid @RequestBody Start form) {
        return Result.success(workflow.saveDraft(access.current(WORK), id, form));
    }

    @GetMapping("/instances/{id}/edit-definition")
    public Result<Definition> editDefinition(@PathVariable String id) {
        return Result.success(workflow.editableDefinition(access.current(WORK), id));
    }

    @PostMapping("/instances/{id}/resubmit")
    public Result<String> resubmit(@PathVariable String id, @Valid @RequestBody Start form) {
        return Result.success(workflow.resubmit(access.current(WORK), id, form));
    }

    @PostMapping("/instances/{id}/attachments")
    public Result<Void> attach(@PathVariable String id, @RequestPart("file") MultipartFile file) throws IOException {
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new ApprovalException("单个附件最大 2 MB");
        }
        workflow.attach(access.current(), id, file.getOriginalFilename(), file.getBytes());
        return Result.success();
    }

    @GetMapping("/instances/{id}/attachments/{attachment}")
    public Result<AttachmentData> attachment(@PathVariable String id, @PathVariable String attachment) {
        return Result.success(workflow.attachment(access.current(), id, attachment));
    }

    @ExceptionHandler(ApprovalException.class)
    public Result<Void> approvalError(ApprovalException exception) {
        return Result.failed(exception.getMessage());
    }
}
