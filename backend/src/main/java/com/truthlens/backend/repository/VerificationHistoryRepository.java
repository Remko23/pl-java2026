package com.truthlens.backend.repository;

import com.truthlens.backend.model.VerificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationHistoryRepository extends MongoRepository<VerificationHistory, String> {
    Page<VerificationHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<VerificationHistory> findByUserIdOrderByCreatedAtAsc(String userId, Pageable pageable);
}
