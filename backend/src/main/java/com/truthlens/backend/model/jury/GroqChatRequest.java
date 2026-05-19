package com.truthlens.backend.model.jury;

import java.util.List;
import java.util.Map;

public record GroqChatRequest(
        String model,
        List<GroqMessage> messages,
        Double temperature,
        Map<String, String> response_format
) {
    public static GroqChatRequest createJsonRequest(String model, List<GroqMessage> messages) {
        return new GroqChatRequest(model, messages, 0.2, Map.of("type", "json_object"));
    }
}
