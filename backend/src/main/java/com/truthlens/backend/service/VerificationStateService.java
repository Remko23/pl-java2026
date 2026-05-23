package com.truthlens.backend.service;

import com.truthlens.backend.model.JuryReport;
import com.truthlens.backend.model.VerificationResponse;
import com.truthlens.backend.model.VerificationStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class VerificationStateService {

    private static final Duration KEY_TTL = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "verification:";

    private final RedisTemplate<String, Object> redisTemplate;

    public VerificationStateService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateState(String verificationId, VerificationStatus status, int progressPercentage, String message, JuryReport result) {
        VerificationResponse response = new VerificationResponse(
                verificationId,
                status,
                progressPercentage,
                message,
                result
        );
        redisTemplate.opsForValue().set(KEY_PREFIX + verificationId, response, KEY_TTL);
    }

    public void updateState(String verificationId, VerificationStatus status, int progressPercentage) {
        updateState(verificationId, status, progressPercentage, null, null);
    }

    public VerificationResponse getState(String verificationId) {
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + verificationId);
        if (value instanceof VerificationResponse) {
            return (VerificationResponse) value;
        }
        return null;
    }
}
