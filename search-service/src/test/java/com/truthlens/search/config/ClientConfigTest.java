package com.truthlens.search.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigTest {

    @Test
    void testDuckDuckGoClientConfig() {
        DuckDuckGoClientConfig config = new DuckDuckGoClientConfig();
        RestClient client = config.duckDuckGoRestClient();
        assertThat(client).isNotNull();
    }

    @Test
    void testWikipediaClientConfig() {
        WikipediaClientConfig config = new WikipediaClientConfig();
        RestClient client = config.wikipediaRestClient();
        assertThat(client).isNotNull();
    }
}
