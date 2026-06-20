package com.truthlens.ocr.model;

import com.truthlens.ocr.model.gemini.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCoverageTest {

    @Test
    void testOcrExtractionResponse() {
        OcrExtractionResponse response = new OcrExtractionResponse("text", true, 80.0);
        assertThat(response.extractedText()).isEqualTo("text");
        assertThat(response.hasManipulationArtifacts()).isTrue();
        assertThat(response.confidenceScore()).isEqualTo(80.0);
        
        assertThat(response.toString()).contains("text");
        assertThat(response.hashCode()).isNotZero();
        assertThat(response).isEqualTo(new OcrExtractionResponse("text", true, 80.0));
    }

    @Test
    void testGeminiModels() {
        GeminiInlineData inlineData = new GeminiInlineData("image/jpeg", "base64");
        assertThat(inlineData.mimeType()).isEqualTo("image/jpeg");
        assertThat(inlineData.data()).isEqualTo("base64");
        assertThat(inlineData.toString()).contains("base64");
        assertThat(inlineData.hashCode()).isNotZero();
        assertThat(inlineData).isEqualTo(new GeminiInlineData("image/jpeg", "base64"));

        GeminiPart part1 = new GeminiPart("prompt", null);
        GeminiPart part2 = new GeminiPart(null, inlineData);
        assertThat(part1.text()).isEqualTo("prompt");
        assertThat(part2.inlineData()).isEqualTo(inlineData);
        assertThat(part1.toString()).contains("prompt");
        assertThat(part1.hashCode()).isNotZero();
        assertThat(part1).isEqualTo(new GeminiPart("prompt", null));

        GeminiContent content = new GeminiContent(List.of(part1, part2));
        assertThat(content.parts()).hasSize(2);
        assertThat(content.toString()).isNotNull();
        assertThat(content.hashCode()).isNotZero();
        assertThat(content).isEqualTo(new GeminiContent(List.of(part1, part2)));

        GeminiCandidate candidate = new GeminiCandidate(content);
        assertThat(candidate.content()).isEqualTo(content);
        assertThat(candidate.toString()).isNotNull();
        assertThat(candidate.hashCode()).isNotZero();
        assertThat(candidate).isEqualTo(new GeminiCandidate(content));

        GeminiRequest request = new GeminiRequest(List.of(content));
        assertThat(request.contents()).hasSize(1);
        assertThat(request.toString()).isNotNull();
        assertThat(request.hashCode()).isNotZero();
        assertThat(request).isEqualTo(new GeminiRequest(List.of(content)));

        GeminiResponse response = new GeminiResponse(List.of(candidate));
        assertThat(response.candidates()).hasSize(1);
        assertThat(response.toString()).isNotNull();
        assertThat(response.hashCode()).isNotZero();
        assertThat(response).isEqualTo(new GeminiResponse(List.of(candidate)));
    }
}
