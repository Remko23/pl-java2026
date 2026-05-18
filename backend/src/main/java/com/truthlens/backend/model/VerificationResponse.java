package com.truthlens.backend.model;

// TODO: Define as Java Record per AI_DEVELOPMENT_GUIDELINES.md §2.1
// Contract from DECISION_LOG_API.md §4.2.A (Response body — 202 Accepted):
//   { "verificationId": "UUID", "status": "QUEUED", "message": "..." }
// Also used for §4.2.B (Polling response — 200 OK):
//   { "verificationId": "UUID", "status": "...", "progressPercentage": N, "result": {...} }

public record VerificationResponse() {
}
