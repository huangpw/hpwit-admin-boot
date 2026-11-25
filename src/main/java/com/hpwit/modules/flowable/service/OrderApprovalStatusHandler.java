package com.hpwit.modules.flowable.service;

import com.hpwit.modules.flowable.model.ApprovalData.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER;
import static com.hpwit.modules.flowable.service.ApprovalBpmn.require;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
class OrderApprovalStatusHandler implements BusinessApprovalStatusHandler {

    private final JdbcClient jdbc;

    @Override
    public String businessPage() {
        return DEMO_ORDER;
    }

    @Override
    public String resolveBusinessId(String businessNo, long applicantId) {
        return legacyBusinessId(businessNo, applicantId);
    }

    @Override
    public String legacyBusinessId(String businessNo, long applicantId) {
        return Long.toString(applicantId);
    }

    @Override
    public void updateStatus(ApprovalReference approval, Status status) {
        if (status == Status.PENDING) {
            trackSubmission(approval);
        } else {
            long orderId = approval.businessId() == null ? approval.applicantId() : Long.parseLong(approval.businessId());
            int updated = jdbc.sql("UPDATE flow_approval_order_status SET status = ? WHERE user_id = ? AND instance_id = ?")
                    .params(status.name(), orderId, approval.instanceId()).update();
            require(approval.businessId() == null || updated == 1, "该订单的当前审批实例已变更");
        }
    }

    private void trackSubmission(ApprovalReference approval) {
        long userId = Long.parseLong(approval.businessId());
        String instanceId = approval.instanceId();
        // Different models can be submitted for the same order. Serialize updates of its current instance.
        jdbc.sql("SELECT id FROM sys_user WHERE id = ? FOR UPDATE").param(userId).query(Long.class).single();
        int updated = jdbc.sql("""
                UPDATE flow_approval_order_status SET instance_id = ?, status = ? WHERE user_id = ? AND status <> ?
                """).params(instanceId, Status.PENDING.name(), userId, Status.PENDING.name()).update();
        if (updated == 0) {
            require(jdbc.sql("SELECT status FROM flow_approval_order_status WHERE user_id = ?")
                    .param(userId).query(String.class).optional().isEmpty(), "该订单已有审批中的申请");
            jdbc.sql("INSERT INTO flow_approval_order_status (user_id, instance_id, status) VALUES (?, ?, ?)")
                    .params(userId, instanceId, Status.PENDING.name()).update();
        }
    }
}
