package com.truthlens.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationResponse(
        String verificationId,
        VerificationStatus status,
        Integer progressPercentage,
        String message,
        JuryReport result
) {
}
