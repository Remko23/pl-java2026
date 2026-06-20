package com.truthlens.backend.service;

import com.truthlens.backend.model.JuryReport;
import com.truthlens.backend.model.VerificationResponse;
import com.truthlens.backend.model.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationStateServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private VerificationStateService stateService;

    @BeforeEach
    void setUp() {
        stateService = new VerificationStateService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testUpdateState_WithReport() {
        JuryReport report = new JuryReport("TRUE", 100.0, "Test");

        stateService.updateState("id123", VerificationStatus.COMPLETED, 100, "Done", report);

        ArgumentCaptor<VerificationResponse> captor = ArgumentCaptor.forClass(VerificationResponse.class);
        verify(valueOperations).set(eq("verification:id123"), captor.capture(), any(Duration.class));

        VerificationResponse response = captor.getValue();
        assertThat(response.verificationId()).isEqualTo("id123");
        assertThat(response.status()).isEqualTo(VerificationStatus.COMPLETED);
        assertThat(response.progressPercentage()).isEqualTo(100);
        assertThat(response.message()).isEqualTo("Done");
        assertThat(response.result()).isEqualTo(report);
    }

    @Test
    void testUpdateState_WithoutReport() {
        stateService.updateState("id123", VerificationStatus.OCR_PROCESSING, 50);

        ArgumentCaptor<VerificationResponse> captor = ArgumentCaptor.forClass(VerificationResponse.class);
        verify(valueOperations).set(eq("verification:id123"), captor.capture(), any(Duration.class));

        VerificationResponse response = captor.getValue();
        assertThat(response.verificationId()).isEqualTo("id123");
        assertThat(response.status()).isEqualTo(VerificationStatus.OCR_PROCESSING);
        assertThat(response.progressPercentage()).isEqualTo(50);
        assertThat(response.message()).isNull();
        assertThat(response.result()).isNull();
    }

    @Test
    void testGetState_Found() {
        VerificationResponse mockResponse = new VerificationResponse("id123", VerificationStatus.COMPLETED, 100, "Done", null);
        when(valueOperations.get("verification:id123")).thenReturn(mockResponse);

        VerificationResponse result = stateService.getState("id123");

        assertThat(result).isNotNull();
        assertThat(result.verificationId()).isEqualTo("id123");
    }

    @Test
    void testGetState_NotFound() {
        when(valueOperations.get("verification:id123")).thenReturn(null);

        VerificationResponse result = stateService.getState("id123");

        assertThat(result).isNull();
    }
}
