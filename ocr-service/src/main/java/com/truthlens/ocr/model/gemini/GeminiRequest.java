package com.truthlens.ocr.model.gemini;
import java.util.List;

public record GeminiRequest(List<GeminiContent> contents) {}