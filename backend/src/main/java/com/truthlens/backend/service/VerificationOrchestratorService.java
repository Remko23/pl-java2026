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
    private final ExecutorService virtualThreadExecutor;

    public VerificationOrchestratorService(VerificationStateService stateService,
                                           GroqLlmService groqLlmService,
                                           JuryVotingService juryVotingService,
                                           SearchServiceClient searchServiceClient,
                                           OcrServiceClient ocrServiceClient,
                                           ObjectMapper objectMapper) {
        this.stateService = stateService;
        this.groqLlmService = groqLlmService;
        this.juryVotingService = juryVotingService;
        this.searchServiceClient = searchServiceClient;
        this.ocrServiceClient = ocrServiceClient;
        this.objectMapper = objectMapper;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public VerificationResponse startVerification(String text) {
        String verificationId = UUID.randomUUID().toString();
        stateService.updateState(verificationId, VerificationStatus.QUEUED, 0, "Weryfikacja została zakolejkowana.", null);

        virtualThreadExecutor.submit(() -> processVerification(verificationId, text));

        return new VerificationResponse(verificationId, VerificationStatus.QUEUED, 0, "Weryfikacja została zakolejkowana.", null);
    }

    public VerificationResponse startVerification(MultipartFile image) {
        String verificationId = UUID.randomUUID().toString();
        stateService.updateState(verificationId, VerificationStatus.QUEUED, 0, "Weryfikacja została zakolejkowana.", null);

        virtualThreadExecutor.submit(() -> {
            try {
                stateService.updateState(verificationId, VerificationStatus.OCR_PROCESSING, 10);
                OcrExtractionResponse ocrResponse = ocrServiceClient.extractText(image);
                
                if (ocrResponse.extractedText() == null || ocrResponse.extractedText().isBlank()) {
                    throw new RuntimeException("Nie udało się odczytać tekstu z obrazu.");
                }
                
                processVerification(verificationId, ocrResponse.extractedText());
            } catch (Exception e) {
                log.error("Failed to process image for verification {}", verificationId, e);
                stateService.updateState(verificationId, VerificationStatus.FAILED, 0, "Błąd przetwarzania obrazu: " + e.getMessage(), null);
            }
        });

        return new VerificationResponse(verificationId, VerificationStatus.QUEUED, 0, "Weryfikacja została zakolejkowana.", null);
    }

    private void processVerification(String verificationId, String text) {
        try {
            // GENERATING_QUERIES
            stateService.updateState(verificationId, VerificationStatus.GENERATING_QUERIES, 30);
            String prompt = "Wygeneruj 3 optymalne zapytania do wyszukiwarki internetowej, aby zweryfikować prawdziwość tego tekstu. Zwróć wyłączenie zserializowaną tablicę JSON stringów i nic więcej (bez formatowania Markdown). Tekst: " + text;
            String rawQueries = groqLlmService.askModel("llama-3.1-8b-instant", prompt);
            
            // Clean markdown block if necessary
            rawQueries = rawQueries.replaceAll("```json", "").replaceAll("```", "").trim();
            String[] queriesArray = objectMapper.readValue(rawQueries, String[].class);
            List<String> queries = Arrays.asList(queriesArray);
            
            // SEARCHING_WEB
            stateService.updateState(verificationId, VerificationStatus.SEARCHING_WEB, 50);
            SearchExecutionResponse searchResponse = searchServiceClient.executeSearch(new SearchExecutionRequest(queries, 3));
            
            String searchEvidence = searchResponse.results().stream()
                    .map(r -> r.title() + " (" + r.url() + "):\n" + r.snippet())
                    .collect(Collectors.joining("\n\n"));

            // AI_JURY_VOTING
            stateService.updateState(verificationId, VerificationStatus.AI_JURY_VOTING, 70);
            List<ModelVoteResult> votes = juryVotingService.gatherVotes(text, searchEvidence);
            
            // COMPLETED
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
            stateService.updateState(verificationId, VerificationStatus.COMPLETED, 100, "Weryfikacja zakończona sukcesem.", report);
            
        } catch (Exception e) {
            log.error("Failed to process verification {}", verificationId, e);
            stateService.updateState(verificationId, VerificationStatus.FAILED, 0, "Wystąpił błąd podczas weryfikacji: " + e.getMessage(), null);
        }
    }
}
