package com.truthlens.backend.model;

// TODO: Define as Java Record per AI_DEVELOPMENT_GUIDELINES.md §2.1
// Represents a single LLM model's vote:
//   { "modelName": "llama-3-70b", "verdict": "FALSE", "confidenceScore": 95, "reasoning": "..." }

public record ModelVoteResult(
        String modelName,
        String verdict,
        Integer confidenceScore,
        String reasoning
) {
}
