package com.truthlens.ocr.model.gemini;
import java.util.List;

public record GeminiResponse(List<GeminiCandidate> candidates) {}

record GeminiCandidate(GeminiContent content) {}