ALTER TABLE flow_approval_status_callback
    ADD COLUMN operation_id VARCHAR(36) NOT NULL DEFAULT '',
    ADD COLUMN operation_payload MEDIUMTEXT,
    DROP INDEX uk_flow_status_callback,
    ADD CONSTRAINT uk_flow_callback_event UNIQUE (instance_id, approval_status, operation_id);
