CREATE TABLE IF NOT EXISTS agnet_ai_chat_session (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(80) NOT NULL DEFAULT '新会话',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_agnet_chat_session_owner_updated (user_id, updated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agnet_ai_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_agnet_chat_message_session (session_id, id),
    INDEX idx_agnet_chat_message_owner (user_id, session_id, id),
    CONSTRAINT chk_agnet_chat_message_role CHECK (role IN ('user', 'assistant')),
    CONSTRAINT fk_agnet_chat_message_session FOREIGN KEY (session_id)
        REFERENCES agnet_ai_chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
