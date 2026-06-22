package com.truthlens.backend.steps;

import com.truthlens.backend.client.GroqApiClient;
import com.truthlens.backend.client.OcrServiceClient;
import com.truthlens.backend.client.SearchServiceClient;
import com.truthlens.backend.repository.VerificationHistoryRepository;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
                "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "groq.api-url=http://localhost:9999",
        "groq.api-key=test-key",
        "groq.models=llama-3.1-8b-instant,llama-3.3-70b-versatile,gemma2-9b-it",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9999/realms/test",
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.InMemoryRedisConfig.class)
public class CucumberSpringConfiguration {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    OcrServiceClient ocrServiceClient;

    @MockitoBean
    SearchServiceClient searchServiceClient;

    @MockitoBean
    GroqApiClient groqApiClient;

    @MockitoBean
    VerificationHistoryRepository historyRepository;

    @MockitoBean
    com.truthlens.backend.repository.UserRepository userRepository;

    @TestConfiguration
    static class InMemoryRedisConfig {

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        public RedisTemplate<String, Object> redisTemplate() {
            ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

            RedisTemplate<String, Object> mockTemplate = Mockito.mock(RedisTemplate.class);
            ValueOperations<String, Object> mockValueOps = Mockito.mock(ValueOperations.class);

            when(mockTemplate.opsForValue()).thenReturn(mockValueOps);

            doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Object value = invocation.getArgument(1);
                store.put(key, value);
                return null;
            }).when(mockValueOps).set(anyString(), any(), any(Duration.class));

            doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Object value = invocation.getArgument(1);
                store.put(key, value);
                return null;
            }).when(mockValueOps).set(anyString(), any());

            when(mockValueOps.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return store.get(key);
            });

            return mockTemplate;
        }
    }
}
