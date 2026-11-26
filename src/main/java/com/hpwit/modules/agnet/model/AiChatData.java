package com.hpwit.modules.agnet.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AiChatData {

    private AiChatData() {
    }

    public record Session(String id, String title, String preview, Instant createdAt, Instant updatedAt) {
    }

    public record Message(long id, String sessionId, String role, String content, Instant createdAt) {
    }

    public record RenameSession(@NotBlank @Size(max = 80) String title) {
    }

    public record SendMessage(@NotBlank @Size(max = 8000) String content) {
    }

    public record Turn(String role, String content) {
    }
}
