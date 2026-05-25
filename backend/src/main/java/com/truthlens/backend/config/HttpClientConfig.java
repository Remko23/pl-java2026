package com.truthlens.backend.config;

import com.truthlens.backend.client.OcrServiceClient;
import com.truthlens.backend.client.SearchServiceClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public SearchServiceClient searchServiceClient(RestClient.Builder builder) {
        RestClient restClient = builder.baseUrl("http://truthlens-search-service").build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SearchServiceClient.class);
    }

    @Bean
    public OcrServiceClient ocrServiceClient(RestClient.Builder builder) {
        RestClient restClient = builder.baseUrl("http://truthlens-ocr-service").build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(OcrServiceClient.class);
    }
}
