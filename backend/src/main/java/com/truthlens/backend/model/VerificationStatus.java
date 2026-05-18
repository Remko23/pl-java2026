package com.truthlens.backend.model;

// Granular verification status enum.
// Used by VerificationStateService to track progress in Redis.
// Frontend maps these values to UI progress indicators.
// See DECISION_LOG_API.md §4.2.B for the full list.

public enum VerificationStatus {
    QUEUED,
    OCR_PROCESSING,
    GENERATING_QUERIES,
    SEARCHING_WEB,
    AI_JURY_VOTING,
    COMPLETED,
    FAILED
}
