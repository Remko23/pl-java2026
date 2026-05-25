package com.truthlens.backend.model;

public record OcrExtractionResponse(String extractedText, boolean hasManipulationArtifacts, double confidenceScore) {
}
