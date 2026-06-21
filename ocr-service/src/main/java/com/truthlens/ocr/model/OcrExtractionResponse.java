package com.truthlens.ocr.model;

public record OcrExtractionResponse(
        String extractedText,
        boolean hasManipulationArtifacts,
        double confidenceScore
) {}