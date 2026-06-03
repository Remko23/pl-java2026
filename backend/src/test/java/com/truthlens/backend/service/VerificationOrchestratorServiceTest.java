package com.truthlens.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.backend.client.OcrServiceClient;
import com.truthlens.backend.client.SearchServiceClient;
import com.truthlens.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationOrchestratorServiceTest {

    @Mock
    private VerificationStateService stateService;
    @Mock
    private GroqLlmService groqLlmService;
    @Mock
    private JuryVotingService juryVotingService;
    @Mock
    private SearchServiceClient searchServiceClient;
    @Mock
    private OcrServiceClient ocrServiceClient;
    @Mock
    private VerificationHistoryService historyService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private VerificationOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        orchestratorService = new VerificationOrchestratorService(
                stateService, groqLlmService, juryVotingService, searchServiceClient, ocrServiceClient, objectMapper, historyService);
    }

    @Test
    void startVerification_withText_shouldProcessSuccessfully() throws Exception {
        String claimText = "Bill Gates adds chips to vaccines";
        String userId = "test-user-id";

        when(groqLlmService.askModel(anyString(), anyString()))
                .thenReturn("[\"vaccine chips fact check\", \"bill gates vaccines\"]");
        when(searchServiceClient.executeSearch(any())).thenReturn(new SearchExecutionResponse(List.of(
                new SearchResult("Title", "http://example.com", "Snippet"))));
        when(juryVotingService.gatherVotes(anyString(), anyString())).thenReturn(List.of(
                new ModelVoteResult("Llama3", "FALSE", 99, "It is a conspiracy theory.")));

        VerificationResponse response = orchestratorService.startVerification(claimText, userId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(VerificationStatus.QUEUED);

        verify(stateService, timeout(2000)).updateState(eq(response.verificationId()), eq(VerificationStatus.COMPLETED),
                eq(100), anyString(), any(JuryReport.class));

        verify(stateService).updateState(eq(response.verificationId()), eq(VerificationStatus.QUEUED), eq(0),
                anyString(), isNull());
        verify(stateService).updateState(eq(response.verificationId()), eq(VerificationStatus.GENERATING_QUERIES),
                eq(30));
        verify(stateService).updateState(eq(response.verificationId()), eq(VerificationStatus.SEARCHING_WEB), eq(50));
        verify(stateService).updateState(eq(response.verificationId()), eq(VerificationStatus.AI_JURY_VOTING), eq(70));
        verify(historyService, timeout(2000)).saveHistory(eq(userId), eq("TEXT"), eq(claimText), isNull(), any(JuryReport.class));
    }

    @Test
    void startVerification_withImage_shouldProcessSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "fake-image".getBytes());
        String extractedText = "Text extracted from image";
        String userId = "test-user-id";

        when(ocrServiceClient.extractText(any())).thenReturn(new OcrExtractionResponse(extractedText, false, 95.0));
        when(groqLlmService.askModel(anyString(), anyString())).thenReturn("[\"extracted text check\"]");
        when(searchServiceClient.executeSearch(any())).thenReturn(new SearchExecutionResponse(List.of()));
        when(juryVotingService.gatherVotes(anyString(), anyString())).thenReturn(List.of(
                new ModelVoteResult("Llama3", "TRUE", 80, "Seems true.")));

        VerificationResponse response = orchestratorService.startVerification(file, userId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(VerificationStatus.QUEUED);

        verify(stateService, timeout(2000)).updateState(eq(response.verificationId()), eq(VerificationStatus.COMPLETED),
                eq(100), anyString(), any(JuryReport.class));

        verify(stateService).updateState(eq(response.verificationId()), eq(VerificationStatus.OCR_PROCESSING), eq(10));
        verify(ocrServiceClient).extractText(any());
        verify(historyService, timeout(2000)).saveHistory(eq(userId), eq("IMAGE"), eq(extractedText), eq("test.png"), any(JuryReport.class));
    }

    @Test
    void startVerification_withText_shouldHandleFailure() {
        String claimText = "Error causing text";
        String userId = "test-user-id";
        when(groqLlmService.askModel(anyString(), anyString())).thenThrow(new RuntimeException("LLM API is down"));

        VerificationResponse response = orchestratorService.startVerification(claimText, userId);
        verify(stateService, timeout(2000)).updateState(eq(response.verificationId()), eq(VerificationStatus.FAILED),
                eq(0), contains("LLM API is down"), isNull());
    }
}
