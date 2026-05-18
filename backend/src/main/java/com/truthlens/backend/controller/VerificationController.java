package com.truthlens.backend.controller;

// TODO: Implement the public verification API.
// Contract from DECISION_LOG_API.md §4.2:
//   POST /api/v1/verifications          -> returns 202 Accepted + verificationId
//   GET  /api/v1/verifications/{id}     -> returns current status from Redis (Polling)
// Secured via JWT (handled by SecurityConfig + Gateway).

public class VerificationController {
}
