package com.truthlens.ocr.model.gemini;
import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiPart(
        String text,
        @JsonProperty("inline_data") GeminiInlineData inlineData
) {}