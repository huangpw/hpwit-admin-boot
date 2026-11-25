CREATE TABLE IF NOT EXISTS flow_approval_business_binding (
    instance_id VARCHAR(64) NOT NULL PRIMARY KEY,
    business_page VARCHAR(64) NOT NULL,
    business_id VARCHAR(191) NOT NULL,
    active_business_id VARCHAR(191),
    CONSTRAINT uk_flow_active_business UNIQUE (business_page, active_business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
