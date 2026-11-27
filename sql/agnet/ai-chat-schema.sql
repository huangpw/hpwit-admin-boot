CREATE TABLE IF NOT EXISTS agnet_ai_chat_session (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '聊天会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    title VARCHAR(80) NOT NULL DEFAULT '新会话' COMMENT '聊天会话标题',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    INDEX idx_agnet_chat_session_owner_updated (user_id, updated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天会话表';

CREATE TABLE IF NOT EXISTS agnet_ai_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '聊天消息ID',
    session_id VARCHAR(36) NOT NULL COMMENT '所属聊天会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    role VARCHAR(16) NOT NULL COMMENT '消息角色（user 用户，assistant 助手）',
    content MEDIUMTEXT NOT NULL COMMENT '消息正文',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    INDEX idx_agnet_chat_message_session (session_id, id),
    INDEX idx_agnet_chat_message_owner (user_id, session_id, id),
    CONSTRAINT chk_agnet_chat_message_role CHECK (role IN ('user', 'assistant')),
    CONSTRAINT fk_agnet_chat_message_session FOREIGN KEY (session_id)
        REFERENCES agnet_ai_chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天消息表';
