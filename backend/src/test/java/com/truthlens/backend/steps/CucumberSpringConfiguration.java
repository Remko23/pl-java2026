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
import org.springframework.boot.test.mock.mockito.MockBean;
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

/**
 * Cucumber-Spring glue configuration.
 * <p>
 * Boots the full Spring context with MockMvc available for REST assertions.
 * External infrastructure (Config Server, Eureka, MongoDB, PostgreSQL)
 * is disabled via properties so tests run without Docker or network access.
 * <p>
 * Redis is replaced by an in-memory {@link ConcurrentHashMap}-backed mock
 * so that {@code VerificationStateService} can store and retrieve state
 * throughout the async verification flow without a real Redis instance.
 * <p>
 * All {@code @MockBean} annotations must live on this class (the
 * {@code @CucumberContextConfiguration} class) because cucumber-spring
 * uses this class — not the step definition classes — when bootstrapping
 * the Spring Test context.
 */
@CucumberContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
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
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.InMemoryRedisConfig.class)
public class CucumberSpringConfiguration {

    // ─── MockBeans must be declared HERE (on the @CucumberContextConfiguration class) ───

    /** Keycloak JWT decoder — mocked so no real Keycloak instance is needed. */
    @MockBean
    JwtDecoder jwtDecoder;

    /** OCR microservice client — not used in text-only flow, mocked to satisfy DI. */
    @MockBean
    OcrServiceClient ocrServiceClient;

    /** Search microservice client — stubbed to return contradicting web results. */
    @MockBean
    SearchServiceClient searchServiceClient;

    /** Groq LLM API client — stubbed to return AI jury vote JSON responses. */
    @MockBean
    GroqApiClient groqApiClient;

    /** MongoDB history repository — mocked to avoid needing a real database. */
    @MockBean
    VerificationHistoryRepository historyRepository;

    /** JPA user repository — mocked to avoid needing a real database. */
    @MockBean
    com.truthlens.backend.repository.UserRepository userRepository;

    /**
     * Provides an in-memory {@link RedisTemplate} mock that backs its
     * {@link ValueOperations} with a {@link ConcurrentHashMap}.
     * <p>
     * This allows {@code VerificationStateService} to work with real logic
     * (set/get keys) without needing a running Redis server.
     */
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

            // set(key, value, ttl) → store in ConcurrentHashMap
            doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Object value = invocation.getArgument(1);
                store.put(key, value);
                return null;
            }).when(mockValueOps).set(anyString(), any(), any(Duration.class));

            // set(key, value) → store in ConcurrentHashMap
            doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Object value = invocation.getArgument(1);
                store.put(key, value);
                return null;
            }).when(mockValueOps).set(anyString(), any());

            // get(key) → retrieve from ConcurrentHashMap
            when(mockValueOps.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return store.get(key);
            });

            return mockTemplate;
        }
    }
}
