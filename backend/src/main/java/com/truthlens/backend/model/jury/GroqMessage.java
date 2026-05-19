package com.truthlens.backend.model.jury;

public record GroqMessage(
        String role,
        String content
) {
}
