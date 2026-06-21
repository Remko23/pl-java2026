package com.truthlens.backend.model;

public record ModelVoteResult(
        String modelName,
        String verdict,
        Integer confidenceScore,
        String reasoning
) {
}
