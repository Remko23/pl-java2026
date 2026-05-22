package com.truthlens.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableRetry
public class DuckDuckGoClientConfig {

    private static final String DUCKDUCKGO_API_BASE_URL = "https://api.duckduckgo.com";

    @Bean
    public RestClient duckDuckGoRestClient() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(new MediaType("application", "x-javascript"));
        converter.setSupportedMediaTypes(supportedMediaTypes);

        return RestClient.builder()
                .baseUrl(DUCKDUCKGO_API_BASE_URL)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(converter);
                })
                .build();
    }
}
