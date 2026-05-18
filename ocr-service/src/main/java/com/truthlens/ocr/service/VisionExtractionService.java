package com.truthlens.ocr.service;

// TODO: Implement Vision AI integration.
// Responsibilities:
//   - Accept image bytes
//   - Call external Vision API (Gemini Flash Vision) with a prompt:
//     "Extract all text from this image. Note any obvious artifacts suggesting cheap photomontage."
//   - Parse the response into OcrExtractionResponse
//   - Apply @Retryable for transient failures (429, 503)
//   - Throw 422 Unprocessable Entity for corrupted/unreadable images

public class VisionExtractionService {
}
