package com.truthlens.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCoverageTest {

    @Test
    void testUserEntity() {
        User user = new User("kc123", "test@example.com", "John", "Doe");
        user.setId(1L);
        user.setKeycloakId("kc123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getKeycloakId()).isEqualTo("kc123");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getCreatedAt()).isNull(); // Not set because PrePersist not called
    }

    @Test
    void testVerificationHistoryEntity() {
        VerificationHistory history = new VerificationHistory("u1", "TEXT", "text", "file.png", "TRUE", 90.0, "reason");
        history.setId("h1");
        history.setUserId("u1");
        history.setInputType("TEXT");
        history.setInputText("text");
        history.setFileName("file.png");
        history.setFinalVerdict("TRUE");
        history.setAverageConfidence(90.0);
        history.setAggregatedReasoning("reason");
        LocalDateTime now = LocalDateTime.now();
        history.setCreatedAt(now);
        
        assertThat(history.getId()).isEqualTo("h1");
        assertThat(history.getUserId()).isEqualTo("u1");
        assertThat(history.getInputType()).isEqualTo("TEXT");
        assertThat(history.getInputText()).isEqualTo("text");
        assertThat(history.getFileName()).isEqualTo("file.png");
        assertThat(history.getFinalVerdict()).isEqualTo("TRUE");
        assertThat(history.getAverageConfidence()).isEqualTo(90.0);
        assertThat(history.getAggregatedReasoning()).isEqualTo("reason");
        assertThat(history.getCreatedAt()).isEqualTo(now);
    }
}
