package com.truthlens.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.backend.client.OcrServiceClient;
import com.truthlens.backend.client.SearchServiceClient;
import com.truthlens.backend.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class VerificationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(VerificationOrchestratorService.class);

    private final VerificationStateService stateService;
    private final GroqLlmService groqLlmService;
    private final JuryVotingService juryVotingService;
    private final SearchServiceClient searchServiceClient;
    private final OcrServiceClient ocrServiceClient;
    private final ObjectMapper objectMapper;
    private final VerificationHistoryService historyService;
    private final ExecutorService virtualThreadExecutor;

    public VerificationOrchestratorService(VerificationStateService stateService,
            GroqLlmService groqLlmService,
            JuryVotingService juryVotingService,
            SearchServiceClient searchServiceClient,
            OcrServiceClient ocrServiceClient,
            ObjectMapper objectMapper,
            VerificationHistoryService historyService) {
        this.stateService = stateService;
        this.groqLlmService = groqLlmService;
        this.juryVotingService = juryVotingService;
        this.searchServiceClient = searchServiceClient;
        this.ocrServiceClient = ocrServiceClient;
        this.objectMapper = objectMapper;
        this.historyService = historyService;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public VerificationResponse startVerification(String text, String userId) {
        String verificationId = UUID.randomUUID().toString();
        stateService.updateState(verificationId, VerificationStatus.QUEUED, 0, "Verification in progress...",
                null);

        virtualThreadExecutor.submit(() -> processVerification(verificationId, text, userId, "TEXT", null));

        return new VerificationResponse(verificationId, VerificationStatus.QUEUED, 0,
                "Verification in progress...", null);
    }

    public VerificationResponse startVerification(MultipartFile image, String userId) {
        String verificationId = UUID.randomUUID().toString();
        stateService.updateState(verificationId, VerificationStatus.QUEUED, 0, "Verification in progress...",
                null);

        byte[] imageBytes;
        String originalFilename;
        try {
            imageBytes = image.getBytes();
            originalFilename = image.getOriginalFilename();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read image bytes", e);
        }

        virtualThreadExecutor.submit(() -> {
            try {
                stateService.updateState(verificationId, VerificationStatus.OCR_PROCESSING, 10);
                org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(
                        imageBytes) {
                    @Override
                    public String getFilename() {
                        return originalFilename != null ? originalFilename : "image.png";
                    }
                };
                OcrExtractionResponse ocrResponse = ocrServiceClient.extractText(resource);

                if (ocrResponse.extractedText() == null || ocrResponse.extractedText().isBlank()) {
                    throw new RuntimeException("Failed to read text from image.");
                }

                processVerification(verificationId, ocrResponse.extractedText(), userId, "IMAGE", originalFilename);
            } catch (Exception e) {
                log.error("Failed to process image for verification {}", verificationId, e);
                stateService.updateState(verificationId, VerificationStatus.FAILED, 0,
                        "Image processing error: " + e.getMessage(), null);
            }
        });

        return new VerificationResponse(verificationId, VerificationStatus.QUEUED, 0,
                "Verification in progress...", null);
    }

    private void processVerification(String verificationId, String text, String userId, String inputType, String fileName) {
        try {
            stateService.updateState(verificationId, VerificationStatus.GENERATING_QUERIES, 30);
            String prompt = "Wygeneruj 3 optymalne zapytania do wyszukiwarki internetowej, aby zweryfikować prawdziwość tego tekstu. Zwróć wyłączenie zserializowaną tablicę JSON stringów i nic więcej (bez formatowania Markdown). Tekst: "
                    + text;
            String rawQueries = groqLlmService.askModel("llama-3.1-8b-instant", prompt);

            rawQueries = rawQueries.replaceAll("```json", "").replaceAll("```", "").trim();
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(rawQueries);
            List<String> queries = new java.util.ArrayList<>();
            if (rootNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                    queries.add(parseQueryNode(node));
                }
            } else if (rootNode.isObject()) {
                java.util.Iterator<com.fasterxml.jackson.databind.JsonNode> elements = rootNode.elements();
                while (elements.hasNext()) {
                    com.fasterxml.jackson.databind.JsonNode node = elements.next();
                    if (node.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode el : node) {
                            queries.add(parseQueryNode(el));
                        }
                        break;
                    }
                }
            }
            queries.removeIf(String::isBlank);
            if (queries.isEmpty()) {
                queries.add(text); // Fallback w razie pustego wyniku
            }

            stateService.updateState(verificationId, VerificationStatus.SEARCHING_WEB, 50);
            SearchExecutionResponse searchResponse = searchServiceClient
                    .executeSearch(new SearchExecutionRequest(queries, 3));

            String searchEvidence = searchResponse.results().stream()
                    .map(r -> r.title() + " (" + r.url() + "):\n" + r.snippet())
                    .collect(Collectors.joining("\n\n"));

            stateService.updateState(verificationId, VerificationStatus.AI_JURY_VOTING, 70);
            List<ModelVoteResult> votes = juryVotingService.gatherVotes(text, searchEvidence);

            double avgConfidence = votes.stream().mapToInt(ModelVoteResult::confidenceScore).average().orElse(0.0);
            String aggregatedReasoning = votes.stream()
                    .map(v -> v.modelName() + ": " + v.reasoning())
                    .collect(Collectors.joining("\n"));

            long trueVotes = votes.stream().filter(v -> {
                try {
                    return Integer.parseInt(v.verdict()) > 50;
                } catch (Exception e) {
                    return "TRUE".equalsIgnoreCase(v.verdict());
                }
            }).count();
            String finalVerdict = (trueVotes >= 2) ? "TRUE" : "FALSE";

            JuryReport report = new JuryReport(finalVerdict, avgConfidence, aggregatedReasoning);
            stateService.updateState(verificationId, VerificationStatus.COMPLETED, 100,
                    "Verification completed successfully.", report);

            // Save to history (only successful verifications for logged-in users)
            if (userId != null && !userId.isBlank()) {
                historyService.saveHistory(userId, inputType, text, fileName, report);
            }

        } catch (Exception e) {
            log.error("Failed to process verification {}", verificationId, e);
            stateService.updateState(verificationId, VerificationStatus.FAILED, 0,
                    "An error occurred during verification: " + e.getMessage(), null);
        }
    }

    private String parseQueryNode(com.fasterxml.jackson.databind.JsonNode el) {
        if (el.isObject()) {
            if (el.has("query"))
                return el.get("query").asText();
            if (el.has("q"))
                return el.get("q").asText();
            if (el.has("zapytanie"))
                return el.get("zapytanie").asText();
        } else if (el.isTextual()) {
            String text = el.asText().trim();
            if (text.startsWith("{")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode nested = objectMapper.readTree(text);
                    if (nested.has("query"))
                        return nested.get("query").asText();
                    if (nested.has("q"))
                        return nested.get("q").asText();
                    if (nested.has("zapytanie"))
                        return nested.get("zapytanie").asText();
                } catch (Exception ignored) {
                }
            }
            return text;
        }
        return el.asText();
    }
}
