package com.truthlens.backend.service;

import com.truthlens.backend.client.GroqApiClient;
import com.truthlens.backend.config.GroqProperties;
import com.truthlens.backend.model.jury.GroqChatRequest;
import com.truthlens.backend.model.jury.GroqChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroqLlmServiceTest {

    @Mock
    private GroqApiClient groqApiClient;

    @Mock
    private GroqProperties groqProperties;

    private GroqLlmService groqLlmService;

    @BeforeEach
    void setUp() {
        groqLlmService = new GroqLlmService(groqApiClient, groqProperties);
    }

    @Test
    void testAskModel_Success() {
        when(groqProperties.apiKey()).thenReturn("test-api-key");
        
        GroqChatResponse.Choice choice = new GroqChatResponse.Choice(
                new com.truthlens.backend.model.jury.GroqMessage("assistant", "response content")
        );
        
        GroqChatResponse response = new GroqChatResponse(List.of(choice));

        when(groqApiClient.getChatCompletion(eq("Bearer test-api-key"), any(GroqChatRequest.class))).thenReturn(response);

        String result = groqLlmService.askModel("llama3", "hello");

        assertThat(result).isEqualTo("response content");
    }

    @Test
    void testAskModel_EmptyChoices() {
        when(groqProperties.apiKey()).thenReturn("test-api-key");
        
        GroqChatResponse response = new GroqChatResponse(List.of());

        when(groqApiClient.getChatCompletion(eq("Bearer test-api-key"), any(GroqChatRequest.class))).thenReturn(response);

        String result = groqLlmService.askModel("llama3", "hello");

        assertThat(result).isNull();
    }
}
