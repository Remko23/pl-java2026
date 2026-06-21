package com.truthlens.ocr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.ocr.model.OcrExtractionResponse;
import com.truthlens.ocr.model.gemini.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class VisionExtractionService {

    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;


    public VisionExtractionService(RestClient geminiRestClient, ObjectMapper objectMapper) {
        this.geminiRestClient = geminiRestClient;
        this.objectMapper = objectMapper;
    }

    @Retryable(
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public OcrExtractionResponse extractText(MultipartFile image) {
        validateImage(image);

        String base64Image;
        try {
            base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Nie można odczytać pliku obrazu.", e);
        }

        String mimeType = image.getContentType();
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        String prompt = """
                Extract all text from this image. Note any obvious artifacts suggesting cheap photomontage.
                Return the response STRICTLY as a JSON object with the following keys:
                - "extractedText": (string) The full text found in the image. Leave empty if no text.
                - "hasManipulationArtifacts": (boolean) true if obvious montage artifacts are found, false otherwise.
                - "confidenceScore": (number) 0 to 100 rating your confidence.
                Ensure the output is pure JSON without Markdown formatting.
                """;

        GeminiRequest requestBody = new GeminiRequest(List.of(
                new GeminiContent(List.of(
                        new GeminiPart(prompt, null),
                        new GeminiPart(null, new GeminiInlineData(mimeType, base64Image))
                ))
        ));

        GeminiResponse response = geminiRestClient.post()
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        return parseGeminiResponse(response);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Przesłany plik graficzny jest pusty.");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Przesłany plik nie jest obsługiwanym obrazem.");
        }
    }

    private OcrExtractionResponse parseGeminiResponse(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Otrzymano pustą odpowiedź od modelu AI.");
        }

        String rawJson = response.candidates().get(0).content().parts().get(0).text();

        if (rawJson.startsWith("```json")) {
            rawJson = rawJson.replace("```json", "").replace("```", "").trim();
        } else if (rawJson.startsWith("```")) {
            rawJson = rawJson.replace("```", "").trim();
        }

        try {
            OcrExtractionResponse extracted = objectMapper.readValue(rawJson, OcrExtractionResponse.class);

            if (extracted.extractedText() == null || extracted.extractedText().trim().isEmpty()) {
                throw new IllegalArgumentException("Z przesłanego obrazu nie da się odczytać żadnego tekstu.");
            }

            return extracted;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nie udało się sparsować odpowiedzi JSON od modelu Gemini.", e);
        }
    }
}