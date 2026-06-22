package com.truthlens.backend.service;

import com.truthlens.backend.model.JuryReport;
import com.truthlens.backend.model.VerificationHistory;
import com.truthlens.backend.repository.VerificationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationHistoryServiceTest {

    @Mock
    private VerificationHistoryRepository historyRepository;

    private VerificationHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new VerificationHistoryService(historyRepository);
    }

    @Test
    void testSaveHistory() {
        JuryReport report = new JuryReport("TRUE", 85.0, "Reasoning test");

        historyService.saveHistory("user123", "TEXT", "claim text", null, report);

        ArgumentCaptor<VerificationHistory> captor = ArgumentCaptor.forClass(VerificationHistory.class);
        verify(historyRepository).save(captor.capture());

        VerificationHistory saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user123");
        assertThat(saved.getInputType()).isEqualTo("TEXT");
        assertThat(saved.getInputText()).isEqualTo("claim text");
        assertThat(saved.getFileName()).isNull();
        assertThat(saved.getFinalVerdict()).isEqualTo("TRUE");
        assertThat(saved.getAverageConfidence()).isEqualTo(85.0);
        assertThat(saved.getAggregatedReasoning()).isEqualTo("Reasoning test");
    }

    @Test
    void testGetHistory() {
        Page<VerificationHistory> page = new PageImpl<>(List.of(new VerificationHistory()));
        Pageable pageable = Pageable.unpaged();

        when(historyRepository.findByUserIdOrderByCreatedAtDesc(eq("user123"), eq(pageable))).thenReturn(page);

        Page<VerificationHistory> result = historyService.getHistory("user123", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(historyRepository).findByUserIdOrderByCreatedAtDesc("user123", pageable);
    }
}
