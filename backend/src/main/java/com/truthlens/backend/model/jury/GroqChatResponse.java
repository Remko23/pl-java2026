package com.truthlens.backend.model.jury;

import java.util.List;

public record GroqChatResponse(
        List<Choice> choices
) {
    public record Choice(
            GroqMessage message
    ) {
    }

    public String extractContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).message() != null) {
            return choices.get(0).message().content();
        }
        return null;
    }
}
