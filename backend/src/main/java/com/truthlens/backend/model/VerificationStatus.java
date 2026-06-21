package com.truthlens.backend.model;

public enum VerificationStatus {
    QUEUED,
    OCR_PROCESSING,
    GENERATING_QUERIES,
    SEARCHING_WEB,
    AI_JURY_VOTING,
    COMPLETED,
    FAILED
}
