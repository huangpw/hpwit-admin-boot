package com.hpwit.modules.flowable.service;

import com.hpwit.modules.flowable.model.ApprovalData.Status;

/**
 * A Spring bean that writes approval status to the business data for one page code.
 */
public interface BusinessApprovalStatusHandler {

    record ApprovalReference(String businessNo, String instanceId, long applicantId, String businessId,
                             Integer businessStatus) {
    }

    String businessPage();

    /**
     * Resolves the immutable key and checks the applicant's submission permission.
     * Modules whose business number is itself immutable can use the default.
     */
    default String resolveBusinessId(String businessNo, long applicantId) {
        return businessNo;
    }

    /** Looks up the key for an older instance, without checking the historical applicant's current permission. */
    default String legacyBusinessId(String businessNo, long applicantId) {
        return null;
    }

    default boolean supportsStatusMapping() {
        return false;
    }

    /**
     * Runs inside the workflow transaction after the instance status has been written.
     * PENDING identifies a new submission. Terminal states apply to that specific instance.
     * businessId identifies the stored row. It is null for pre-upgrade instances.
     * businessStatus is the optional model mapping. Otherwise the module uses its own mapping.
     */
    void updateStatus(ApprovalReference approval, Status status);
}
