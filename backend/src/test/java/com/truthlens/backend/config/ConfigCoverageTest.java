package com.truthlens.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigCoverageTest {

    @Test
    void testGroqProperties() {
        GroqProperties properties = new GroqProperties("http://api.groq.com", "key123", List.of("model1"));
        assertThat(properties.apiUrl()).isEqualTo("http://api.groq.com");
        assertThat(properties.apiKey()).isEqualTo("key123");
        assertThat(properties.models()).containsExactly("model1");
    }

    @Test
    void testGroqClientConfig() {
        GroqClientConfig config = new GroqClientConfig();
        GroqProperties properties = new GroqProperties("http://api.groq.com", "key", List.of());
        var client = config.groqApiClient(properties);
        assertThat(client).isNotNull();
    }

    @Test
    void testHttpClientConfig() {
        HttpClientConfig config = new HttpClientConfig();
        
        RestClient.Builder builder = config.loadBalancedRestClientBuilder();
        assertThat(builder).isNotNull();
        
        var searchClient = config.searchServiceClient(RestClient.builder());
        assertThat(searchClient).isNotNull();
        
        var ocrClient = config.ocrServiceClient(RestClient.builder());
        assertThat(ocrClient).isNotNull();
    }

    @Test
    void testRedisConfig() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        var template = config.redisTemplate(factory);
        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isEqualTo(factory);
    }
}
