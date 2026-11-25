CREATE TABLE IF NOT EXISTS flow_approval_business_page (
    code VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    component VARCHAR(180) NOT NULL,
    route_path VARCHAR(180) NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO flow_approval_business_page (code, name, component, route_path, enabled, sort_order)
SELECT 'DEMO_ORDER', '测试订单详情', 'flowable/order/approval.vue', '/flowable/order', TRUE, 0
WHERE NOT EXISTS (SELECT 1 FROM flow_approval_business_page WHERE code = 'DEMO_ORDER');

CREATE TABLE IF NOT EXISTS flow_approval_order (
    user_id BIGINT NOT NULL PRIMARY KEY,
    order_json MEDIUMTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_approval_instance_meta (
    instance_id VARCHAR(64) NOT NULL PRIMARY KEY,
    source_id VARCHAR(64),
    replaced_by VARCHAR(64),
    hidden BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_approval_signature (
    instance_id VARCHAR(64) NOT NULL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    return_to BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_approval_notice (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    recipient_id BIGINT NOT NULL,
    kind VARCHAR(12) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_flow_notice_recipient (recipient_id, kind, created_at),
    INDEX idx_flow_notice_instance (instance_id, recipient_id, kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_approval_attachment (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    data MEDIUMBLOB NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_flow_attachment_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE flow_approval_instance SET status = 'PENDING' WHERE status = 'RUNNING';
