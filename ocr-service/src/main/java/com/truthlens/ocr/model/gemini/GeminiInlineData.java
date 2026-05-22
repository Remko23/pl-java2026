package com.truthlens.ocr.model.gemini;
import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiInlineData(
        @JsonProperty("mime_type") String mimeType,
        String data
) {}