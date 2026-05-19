package com.truthlens.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.backend.config.GroqProperties;
import com.truthlens.backend.model.ModelVoteResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Service
public class JuryVotingService {

    private final GroqLlmService groqLlmService;
    private final GroqProperties groqProperties;
    private final ObjectMapper objectMapper;

    public JuryVotingService(GroqLlmService groqLlmService, GroqProperties groqProperties, ObjectMapper objectMapper) {
        this.groqLlmService = groqLlmService;
        this.groqProperties = groqProperties;
        this.objectMapper = objectMapper;
    }

    public List<ModelVoteResult> gatherVotes(String claimText, String searchEvidence) {
        String prompt = """
                Please verify the fact based on the following claim and evidence.
                Return the response EXCLUSIVELY in JSON format with the following keys:
                - "verdict": (integer from 0 meaning completely false to 100 meaning completely true)
                - "confidenceScore": (integer from 0 to 100)
                - "reasoning": (short explanation)

                Claim: %s
                Internet evidence: %s
                """.formatted(claimText, searchEvidence);

        List<ModelVoteResult> results = new ArrayList<>();
        List<String> models = groqProperties.models();

        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            List<StructuredTaskScope.Subtask<ModelVoteResult>> subtasks = models.stream()
                    .map(model -> scope.fork(() -> {
                        String rawResponse = groqLlmService.askModel(model, prompt);

                        try {
                            JsonNode node = objectMapper.readTree(rawResponse);
                            String verdict = node.has("verdict") ? node.get("verdict").asText() : "UNKNOWN";
                            Integer confidenceScore = node.has("confidenceScore") ? node.get("confidenceScore").asInt()
                                    : 0;
                            String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : rawResponse;

                            return new ModelVoteResult(model, verdict, confidenceScore, reasoning);
                        } catch (Exception e) {
                            return new ModelVoteResult(model, "ERROR", 0, "Failed to parse JSON: " + rawResponse);
                        }
                    }))
                    .toList();

            scope.join();
            
            subtasks.forEach(subtask -> results.add(subtask.get()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Jury voting was interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Jury voting failed", e);
        }

        return results;
    }
}
