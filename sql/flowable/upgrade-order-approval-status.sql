CREATE TABLE IF NOT EXISTS flow_approval_order_status (
    user_id BIGINT NOT NULL PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Each account's order follows its most recently submitted approval.
INSERT INTO flow_approval_order_status (user_id, instance_id, status)
SELECT recent.applicant_id, recent.id, CASE WHEN recent.status = 'RUNNING' THEN 'PENDING' ELSE recent.status END
FROM (
    SELECT i.applicant_id, i.id, i.status,
           ROW_NUMBER() OVER (PARTITION BY i.applicant_id ORDER BY i.started_at DESC, a.id DESC, i.id DESC) AS row_rank
    FROM flow_approval_instance i
    LEFT JOIN flow_approval_action a ON a.instance_id = i.id AND a.action = 'START'
    WHERE i.business_no = 'SO-DEMO-001' AND i.status <> 'DRAFT'
) recent
WHERE recent.row_rank = 1
  AND NOT EXISTS (SELECT 1 FROM flow_approval_order_status s WHERE s.user_id = recent.applicant_id);
