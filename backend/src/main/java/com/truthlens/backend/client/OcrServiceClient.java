package com.truthlens.backend.client;

// TODO: Implement as Spring HTTP Interface per AI_DEVELOPMENT_GUIDELINES.md §2.3
// Uses @PostExchange to call ocr-service via Eureka load balancing.
// Contract from DECISION_LOG_API.md §4.4:
//   POST /api/internal/v1/ocr:extract (multipart/form-data)
// Must be registered as a bean backed by RestClient + Eureka.

public interface OcrServiceClient {
}
