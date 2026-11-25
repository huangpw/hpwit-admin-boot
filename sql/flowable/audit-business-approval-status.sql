-- Historical rejections whose final decision was a return.
SELECT i.id, i.business_no, i.applicant_id, i.status, a.action, a.created_at
FROM flow_approval_instance i
JOIN flow_approval_action a ON a.id = (
    SELECT MAX(last_decision.id) FROM flow_approval_action last_decision
    WHERE last_decision.instance_id = i.id
      AND last_decision.action IN ('RETURN', 'REJECT', 'FORCE_REJECT')
)
WHERE i.status = 'REJECTED' AND a.action = 'RETURN';

-- Test-order status rows that do not match their linked instance.
SELECT s.user_id, s.instance_id, s.status AS business_status, i.status AS workflow_status
FROM flow_approval_order_status s
LEFT JOIN flow_approval_instance i ON i.id = s.instance_id
WHERE i.id IS NULL OR i.applicant_id <> s.user_id OR i.business_no <> 'SO-DEMO-001'
   OR s.status <> CASE WHEN i.status = 'RUNNING' THEN 'PENDING' ELSE i.status END;

-- Approvals created before the immutable business binding was introduced.
SELECT i.id, i.business_no, i.applicant_id, i.definition_id, i.started_at
FROM flow_approval_instance i
LEFT JOIN flow_approval_business_binding b ON b.instance_id = i.id
WHERE i.status IN ('PENDING', 'RUNNING') AND i.business_no <> '' AND b.instance_id IS NULL;
