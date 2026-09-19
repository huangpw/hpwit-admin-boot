-- ============================================================
-- nova-admin-boot 系统配置表 seed
-- sys_config：系统级参数（当前唯一消费方为 IP 限流过滤器 RateLimiterFilter）
-- DDL 与预置数据移植自 youlai-boot youlai_admin.sql 的 sys_config 部分
-- 适用 MySQL 8.x（utf8mb4 / InnoDB），可重复执行
-- ============================================================

-- ----- sys_config 系统配置表 -----
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_name  VARCHAR(50)   NOT NULL COMMENT '配置名称',
    config_key   VARCHAR(50)   NOT NULL COMMENT '配置键',
    config_value VARCHAR(100)  NOT NULL COMMENT '配置值',
    remark       VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                 DEFAULT NULL COMMENT '创建人ID',
    update_by    BIGINT                 DEFAULT NULL COMMENT '更新人ID',
    is_deleted   TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
    create_time  DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----- 预置：IP 限流 QPS 阈值（RateLimiterFilter 消费） -----
INSERT INTO sys_config (config_name, config_key, config_value, remark, create_by)
SELECT '系统限流QPS', 'IP_QPS_THRESHOLD_LIMIT', '10',
       '单个IP请求的最大每秒查询数（QPS）阈值', 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'IP_QPS_THRESHOLD_LIMIT'
);
