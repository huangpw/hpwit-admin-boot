package com.hpwit.modules.agnet.service;

import com.hpwit.modules.agnet.model.AiChatData.Message;
import com.hpwit.modules.agnet.model.AiChatData.Session;
import com.hpwit.modules.agnet.model.AiChatData.Turn;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class AiChatStore {

    private final JdbcClient jdbc;

    List<Session> sessions(long userId) {
        return jdbc.sql("""
                SELECT s.id, s.title, s.created_at, s.updated_at,
                       (SELECT LEFT(m.content, 180) FROM agnet_ai_chat_message m
                        WHERE m.session_id = s.id ORDER BY m.id DESC LIMIT 1) AS preview
                FROM agnet_ai_chat_session s
                WHERE s.user_id = ?
                ORDER BY s.updated_at DESC, s.id DESC
                LIMIT 100
                """).param(userId).query(AiChatStore::sessionRow).list();
    }

    Session create(long userId) {
        String id = UUID.randomUUID().toString();
        jdbc.sql("INSERT INTO agnet_ai_chat_session (id, user_id, title) VALUES (?, ?, ?)")
                .params(id, userId, "新会话").update();
        return session(userId, id).orElseThrow();
    }

    Optional<Session> session(long userId, String id) {
        return jdbc.sql("""
                SELECT s.id, s.title, s.created_at, s.updated_at,
                       (SELECT LEFT(m.content, 180) FROM agnet_ai_chat_message m
                        WHERE m.session_id = s.id ORDER BY m.id DESC LIMIT 1) AS preview
                FROM agnet_ai_chat_session s WHERE s.user_id = ? AND s.id = ?
                """).params(userId, id).query(AiChatStore::sessionRow).optional();
    }

    List<Message> messages(long userId, String sessionId) {
        return jdbc.sql("""
                SELECT m.id, m.session_id, m.role, m.content, m.created_at
                FROM agnet_ai_chat_message m
                JOIN agnet_ai_chat_session s ON s.id = m.session_id AND s.user_id = ?
                WHERE m.session_id = ?
                ORDER BY m.id DESC LIMIT 200
                """).params(userId, sessionId).query(AiChatStore::messageRow)
                .list().stream().sorted((a, b) -> Long.compare(a.id(), b.id())).toList();
    }

    List<Turn> context(String sessionId) {
        List<Turn> turns = jdbc.sql("""
                SELECT role, content FROM agnet_ai_chat_message
                WHERE session_id = ? ORDER BY id DESC LIMIT 24
                """).param(sessionId).query((rs, row) -> new Turn(rs.getString("role"), rs.getString("content"))).list();
        List<Turn> ordered = new ArrayList<>(turns);
        java.util.Collections.reverse(ordered);
        return ordered;
    }

    void addUserMessage(long userId, String sessionId, String content) {
        jdbc.sql("INSERT INTO agnet_ai_chat_message (session_id, user_id, role, content) VALUES (?, ?, 'user', ?)")
                .params(sessionId, userId, content).update();
        String title = content.length() > 80 ? content.substring(0, 79) + "…" : content;
        jdbc.sql("""
                UPDATE agnet_ai_chat_session SET
                    title = CASE WHEN title = '新会话' THEN ? ELSE title END,
                    updated_at = CURRENT_TIMESTAMP(3)
                WHERE id = ? AND user_id = ?
                """).params(title, sessionId, userId).update();
    }

    void addAssistantMessage(long userId, String sessionId, String content) {
        jdbc.sql("INSERT INTO agnet_ai_chat_message (session_id, user_id, role, content) VALUES (?, ?, 'assistant', ?)")
                .params(sessionId, userId, content).update();
        jdbc.sql("UPDATE agnet_ai_chat_session SET updated_at = CURRENT_TIMESTAMP(3) WHERE id = ? AND user_id = ?")
                .params(sessionId, userId).update();
    }

    boolean rename(long userId, String sessionId, String title) {
        return jdbc.sql("UPDATE agnet_ai_chat_session SET title = ?, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ? AND user_id = ?")
                .params(title.strip(), sessionId, userId).update() != 0;
    }

    boolean delete(long userId, String sessionId) {
        jdbc.sql("DELETE FROM agnet_ai_chat_message WHERE session_id = ? AND user_id = ?")
                .params(sessionId, userId).update();
        return jdbc.sql("DELETE FROM agnet_ai_chat_session WHERE id = ? AND user_id = ?")
                .params(sessionId, userId).update() != 0;
    }

    private static Session sessionRow(ResultSet rs, int row) throws SQLException {
        return new Session(rs.getString("id"), rs.getString("title"), rs.getString("preview"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private static Message messageRow(ResultSet rs, int row) throws SQLException {
        return new Message(rs.getLong("id"), rs.getString("session_id"), rs.getString("role"),
                rs.getString("content"), instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String field) throws SQLException {
        var timestamp = rs.getTimestamp(field);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
