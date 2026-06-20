package com.truthlens.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.backend.config.GroqProperties;
import com.truthlens.backend.model.ModelVoteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JuryVotingServiceTest {

    @Mock
    private GroqLlmService groqLlmService;

    @Mock
    private GroqProperties groqProperties;

    private ObjectMapper objectMapper = new ObjectMapper();

    private JuryVotingService juryVotingService;

    @BeforeEach
    void setUp() {
        juryVotingService = new JuryVotingService(groqLlmService, groqProperties, objectMapper);
    }

    @Test
    void testGatherVotes_Success() {
        when(groqProperties.models()).thenReturn(List.of("model-1", "model-2"));

        String mockResponse1 = "{\"verdict\": 80, \"confidenceScore\": 90, \"reasoning\": \"Seems true\"}";
        String mockResponse2 = "{\"verdict\": 20, \"confidenceScore\": 85, \"reasoning\": \"Seems false\"}";

        when(groqLlmService.askModel(eq("model-1"), anyString())).thenReturn(mockResponse1);
        when(groqLlmService.askModel(eq("model-2"), anyString())).thenReturn(mockResponse2);

        List<ModelVoteResult> results = juryVotingService.gatherVotes("claim", "evidence");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ModelVoteResult::modelName).containsExactlyInAnyOrder("model-1", "model-2");
        
        ModelVoteResult res1 = results.stream().filter(r -> r.modelName().equals("model-1")).findFirst().get();
        assertThat(res1.verdict()).isEqualTo("80");
        assertThat(res1.confidenceScore()).isEqualTo(90);
        assertThat(res1.reasoning()).isEqualTo("Seems true");
    }

    @Test
    void testGatherVotes_MalformedJson() {
        when(groqProperties.models()).thenReturn(List.of("model-1"));
        when(groqLlmService.askModel(eq("model-1"), anyString())).thenReturn("Not JSON");

        List<ModelVoteResult> results = juryVotingService.gatherVotes("claim", "evidence");

        assertThat(results).hasSize(1);
        ModelVoteResult res = results.get(0);
        assertThat(res.verdict()).isEqualTo("ERROR");
        assertThat(res.confidenceScore()).isEqualTo(0);
        assertThat(res.reasoning()).contains("Failed to parse JSON");
    }

    @Test
    void testGatherVotes_MissingFields() {
        when(groqProperties.models()).thenReturn(List.of("model-1"));
        when(groqLlmService.askModel(eq("model-1"), anyString())).thenReturn("{}");

        List<ModelVoteResult> results = juryVotingService.gatherVotes("claim", "evidence");

        assertThat(results).hasSize(1);
        ModelVoteResult res = results.get(0);
        assertThat(res.verdict()).isEqualTo("UNKNOWN");
        assertThat(res.confidenceScore()).isEqualTo(0);
        assertThat(res.reasoning()).isEqualTo("{}");
    }

    @Test
    void testGatherVotes_ThrowsException() {
        when(groqProperties.models()).thenReturn(List.of("model-1"));
        when(groqLlmService.askModel(eq("model-1"), anyString())).thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() -> juryVotingService.gatherVotes("claim", "evidence"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Jury voting failed");
    }
}
