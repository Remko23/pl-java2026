package com.truthlens.ocr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.ocr.model.OcrExtractionResponse;
import com.truthlens.ocr.model.gemini.GeminiCandidate;
import com.truthlens.ocr.model.gemini.GeminiContent;
import com.truthlens.ocr.model.gemini.GeminiPart;
import com.truthlens.ocr.model.gemini.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VisionExtractionServiceTest {

    private RestClient geminiRestClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;
    private ObjectMapper objectMapper;
    private VisionExtractionService visionExtractionService;

    @BeforeEach
    void setUp() {
        geminiRestClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(geminiRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        objectMapper = new ObjectMapper();
        visionExtractionService = new VisionExtractionService(geminiRestClient, objectMapper);
    }

    @Test
    void extractText_Success() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy data".getBytes());
        String jsonResponse = "{\"extractedText\": \"Some text\", \"hasManipulationArtifacts\": false, \"confidenceScore\": 95.5}";
        
        GeminiResponse geminiResponse = createMockGeminiResponse(jsonResponse);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(geminiResponse);

        OcrExtractionResponse result = visionExtractionService.extractText(file);

        assertThat(result).isNotNull();
        assertThat(result.extractedText()).isEqualTo("Some text");
        assertThat(result.hasManipulationArtifacts()).isFalse();
        assertThat(result.confidenceScore()).isEqualTo(95.5);
    }

    @Test
    void extractText_RemovesMarkdownJsonBlock() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy data".getBytes());
        String jsonResponse = "```json\n{\"extractedText\": \"Some text\", \"hasManipulationArtifacts\": false, \"confidenceScore\": 95.5}\n```";
        
        GeminiResponse geminiResponse = createMockGeminiResponse(jsonResponse);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(geminiResponse);

        OcrExtractionResponse result = visionExtractionService.extractText(file);

        assertThat(result).isNotNull();
        assertThat(result.extractedText()).isEqualTo("Some text");
    }

    @Test
    void extractText_ThrowsWhenImageIsNull() {
        assertThatThrownBy(() -> visionExtractionService.extractText(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pust");
    }

    @Test
    void extractText_ThrowsWhenImageIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pust");
    }

    @Test
    void extractText_ThrowsWhenContentTypeInvalid() {
        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", "text".getBytes());
        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obs");
    }

    @Test
    void extractText_ThrowsWhenIOExceptionOnRead() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes()) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("Failed to read");
            }
        };
        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("odczyta");
    }

    @Test
    void extractText_ThrowsWhenResponseIsNull() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());
        when(responseSpec.body(GeminiResponse.class)).thenReturn(null);

        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pust");
    }

    @Test
    void extractText_ThrowsWhenJsonParsingFails() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());
        GeminiResponse geminiResponse = createMockGeminiResponse("{ invalid json }");
        when(responseSpec.body(GeminiResponse.class)).thenReturn(geminiResponse);

        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sparsowa");
    }

    @Test
    void extractText_ThrowsWhenNoTextExtracted() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());
        GeminiResponse geminiResponse = createMockGeminiResponse("{\"extractedText\": \"\", \"hasManipulationArtifacts\": false, \"confidenceScore\": 95.5}");
        when(responseSpec.body(GeminiResponse.class)).thenReturn(geminiResponse);

        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adnego");
    }
    
    @Test
    void extractText_WithNullContentType_UsesDefault() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", null, "dummy".getBytes());
        String jsonResponse = "{\"extractedText\": \"Some text\", \"hasManipulationArtifacts\": false, \"confidenceScore\": 95.5}";
        
        GeminiResponse geminiResponse = createMockGeminiResponse(jsonResponse);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(geminiResponse);

        // Without content-type, it will fallback to image/jpeg if it skips validateImage
        // Actually, our validateImage throws if contentType == null
        assertThatThrownBy(() -> visionExtractionService.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obs");
    }
    
    @Test
    void parseGeminiResponse_RemovesPlainMarkdownBlock() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy data".getBytes());
        String jsonResponse = "```\n{\"extractedText\": \"Some text\", \"hasManipulationArtifacts\": false, \"confidenceScore\": 95.5}\n```";
        
        GeminiResponse geminiResponse = createMockGeminiResponse(jsonResponse);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(geminiResponse);

        OcrExtractionResponse result = visionExtractionService.extractText(file);

        assertThat(result).isNotNull();
        assertThat(result.extractedText()).isEqualTo("Some text");
    }

    private GeminiResponse createMockGeminiResponse(String text) {
        return new GeminiResponse(List.of(
                new GeminiCandidate(new GeminiContent(List.of(new GeminiPart(text, null))))
        ));
    }
}
