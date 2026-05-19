package com.truthlens.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "groq")
public record GroqProperties(
        String apiUrl,
        String apiKey,
        List<String> models
) {
}
