package com.truthlens.backend.service;

import com.truthlens.backend.client.GroqApiClient;
import com.truthlens.backend.config.GroqProperties;
import com.truthlens.backend.model.jury.GroqChatRequest;
import com.truthlens.backend.model.jury.GroqChatResponse;
import com.truthlens.backend.model.jury.GroqMessage;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroqLlmService {

    private final GroqApiClient groqApiClient;
    private final GroqProperties groqProperties;

    public GroqLlmService(GroqApiClient groqApiClient, GroqProperties groqProperties) {
        this.groqApiClient = groqApiClient;
        this.groqProperties = groqProperties;
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public String askModel(String modelName, String prompt) {
        String authHeader = "Bearer " + groqProperties.apiKey();
        List<GroqMessage> messages = List.of(
                new GroqMessage("user", prompt)
        );
        GroqChatRequest request = GroqChatRequest.createJsonRequest(modelName, messages);

        GroqChatResponse response = groqApiClient.getChatCompletion(authHeader, request);
        return response.extractContent();
    }
}
