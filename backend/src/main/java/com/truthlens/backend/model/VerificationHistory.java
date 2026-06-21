package com.truthlens.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "verification_history")
public class VerificationHistory {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String inputType;
    private String inputText;
    private String fileName;

    private String finalVerdict;
    private double averageConfidence;
    private String aggregatedReasoning;

    @Indexed
    private LocalDateTime createdAt;

    public VerificationHistory() {}

    public VerificationHistory(String userId, String inputType, String inputText, String fileName,
                               String finalVerdict, double averageConfidence, String aggregatedReasoning) {
        this.userId = userId;
        this.inputType = inputType;
        this.inputText = inputText;
        this.fileName = fileName;
        this.finalVerdict = finalVerdict;
        this.averageConfidence = averageConfidence;
        this.aggregatedReasoning = aggregatedReasoning;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }

    public String getInputText() { return inputText; }
    public void setInputText(String inputText) { this.inputText = inputText; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFinalVerdict() { return finalVerdict; }
    public void setFinalVerdict(String finalVerdict) { this.finalVerdict = finalVerdict; }

    public double getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }

    public String getAggregatedReasoning() { return aggregatedReasoning; }
    public void setAggregatedReasoning(String aggregatedReasoning) { this.aggregatedReasoning = aggregatedReasoning; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
