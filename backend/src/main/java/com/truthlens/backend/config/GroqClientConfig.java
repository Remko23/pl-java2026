package com.truthlens.backend.config;

import com.truthlens.backend.client.GroqApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class GroqClientConfig {

    @Bean
    public GroqApiClient groqApiClient(GroqProperties groqProperties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(groqProperties.apiUrl())
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(GroqApiClient.class);
    }
}
