package com.truthlens.backend.client;

import com.truthlens.backend.model.jury.GroqChatRequest;
import com.truthlens.backend.model.jury.GroqChatResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;

public interface GroqApiClient {
    @PostExchange("/chat/completions")
    GroqChatResponse getChatCompletion(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GroqChatRequest request
    );
}
