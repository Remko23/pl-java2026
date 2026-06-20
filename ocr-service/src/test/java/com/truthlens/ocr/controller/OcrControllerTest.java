package com.truthlens.ocr.controller;

import com.truthlens.ocr.model.OcrExtractionResponse;
import com.truthlens.ocr.service.VisionExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OcrControllerTest {

    private VisionExtractionService visionExtractionService;
    private OcrController ocrController;

    @BeforeEach
    void setUp() {
        visionExtractionService = mock(VisionExtractionService.class);
        ocrController = new OcrController(visionExtractionService);
    }

    @Test
    void extract() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test data".getBytes());
        OcrExtractionResponse response = new OcrExtractionResponse("text", false, 99.0);
        
        when(visionExtractionService.extractText(file)).thenReturn(response);

        OcrExtractionResponse result = ocrController.extract(file);

        assertThat(result).isEqualTo(response);
        verify(visionExtractionService).extractText(file);
    }
}
