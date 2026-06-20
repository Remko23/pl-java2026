package com.truthlens.ocr.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiClientConfigTest {

    @Test
    void geminiRestClient() {
        GeminiClientConfig config = new GeminiClientConfig("test-key");
        RestClient client = config.geminiRestClient(RestClient.builder());
        assertThat(client).isNotNull();
    }
}
