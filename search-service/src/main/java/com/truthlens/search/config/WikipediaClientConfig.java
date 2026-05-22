package com.truthlens.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@Configuration
@EnableRetry
public class WikipediaClientConfig {

    private static final String WIKIPEDIA_API_BASE_URL = "https://en.wikipedia.org/w/api.php";

    @Bean
    public RestClient wikipediaRestClient() {
        return RestClient.builder()
                .baseUrl(WIKIPEDIA_API_BASE_URL)
                .defaultHeader("User-Agent", "TruthLens/1.0 (contact@example.com)")
                .build();
    }
}
