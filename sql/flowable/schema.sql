CREATE TABLE IF NOT EXISTS flow_approval_model (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    process_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL,
    bpmn_xml MEDIUMTEXT NOT NULL,
    fields_json TEXT NOT NULL,
    revision INT NOT NULL,
    published_revision INT,
    definition_id VARCHAR(64),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_approval_instance (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    model_id VARCHAR(36) NOT NULL,
    definition_id VARCHAR(64) NOT NULL,
    definition_version INT NOT NULL,
    definition_name VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(150) NOT NULL,
    business_no VARCHAR(100) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    values_json MEDIUMTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at TIMESTAMP(3) NULL,
    CONSTRAINT uk_flow_request UNIQUE (model_id, applicant_id, request_id),
    INDEX idx_flow_applicant (applicant_id, started_at, id),
    INDEX idx_flow_started (started_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_approval_action (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    task_name VARCHAR(255),
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(100) NOT NULL,
    action VARCHAR(16) NOT NULL,
    comment_text VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_flow_action_instance (instance_id, id),
    INDEX idx_flow_action_actor (actor_id, instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
