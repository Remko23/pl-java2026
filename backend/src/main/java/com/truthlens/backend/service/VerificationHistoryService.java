package com.truthlens.backend.service;

import com.truthlens.backend.model.JuryReport;
import com.truthlens.backend.model.VerificationHistory;
import com.truthlens.backend.repository.VerificationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class VerificationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(VerificationHistoryService.class);

    private final VerificationHistoryRepository historyRepository;

    public VerificationHistoryService(VerificationHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void saveHistory(String userId, String inputType, String inputText, String fileName, JuryReport report) {
        VerificationHistory entry = new VerificationHistory(
                userId,
                inputType,
                inputText,
                fileName,
                report.finalVerdict(),
                report.averageConfidence(),
                report.aggregatedReasoning()
        );
        historyRepository.save(entry);
        log.info("Saved verification history for user {} (verdict: {})", userId, report.finalVerdict());
    }

    public Page<VerificationHistory> getHistory(String userId, Pageable pageable) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
