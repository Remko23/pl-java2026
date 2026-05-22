package com.truthlens.ocr.model;

// TODO: Define as Java Record per AI_DEVELOPMENT_GUIDELINES.md §2.1
// Contract from DECISION_LOG_API.md §4.4:
//   { "extractedText": "...", "hasManipulationArtifacts": false, "confidenceScore": 98.5 }

public record OcrExtractionResponse(
        String extractedText,
        boolean hasManipulationArtifacts,
        double confidenceScore
) {}