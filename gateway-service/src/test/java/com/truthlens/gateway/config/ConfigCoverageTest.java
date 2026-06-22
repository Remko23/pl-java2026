package com.truthlens.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "truthlens.gateway.cors.allowed-origins=http://localhost:3000",
    "truthlens.gateway.rate-limit.replenish-rate=5",
    "truthlens.gateway.rate-limit.burst-capacity=10",
    "spring.cloud.gateway.enabled=true"
})
class ConfigCoverageTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Autowired
    private KeyResolver smartKeyResolver;

    @Autowired
    private RouteLocator customRouteLocator;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void testRedisRateLimiterConfigured() {
        assertThat(redisRateLimiter).isNotNull();
    }

    @Test
    void testSmartKeyResolver_withValidBearerTokenAndPrincipal() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer some-token")
                .build();
        
        Principal principal = () -> "testUser";
        
        MockServerWebExchange exchange = MockServerWebExchange.builder(request)
                .principal(principal)
                .build();

        Mono<String> keyMono = smartKeyResolver.resolve(exchange);
        StepVerifier.create(keyMono)
                .expectNext("testUser")
                .verifyComplete();
    }

    @Test
    void testSmartKeyResolver_withNoToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<String> keyMono = smartKeyResolver.resolve(exchange);
        StepVerifier.create(keyMono)
                .expectNext("127.0.0.1")
                .verifyComplete();
    }
    
    @Test
    void testSmartKeyResolver_withNoRemoteAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .build();
        
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<String> keyMono = smartKeyResolver.resolve(exchange);
        StepVerifier.create(keyMono)
                .expectNext("anonymous")
                .verifyComplete();
    }
    
    @Test
    void testSmartKeyResolver_withTokenButErrorInPrincipal() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer some-token")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                .build();
        
        ServerWebExchangeMock exchange = new ServerWebExchangeMock(request);

        Mono<String> keyMono = smartKeyResolver.resolve(exchange.getExchange());
        StepVerifier.create(keyMono)
                .expectNext("10.0.0.1")
                .verifyComplete();
    }

    @Test
    void testCustomRouteLocatorConfigured() {
        assertThat(customRouteLocator).isNotNull();
        assertThat(customRouteLocator.getRoutes().collectList().block()).isNotEmpty();
    }

    @Autowired
    private SecurityWebFilterChain springSecurityFilterChain;

    @Test
    void testSecurityConfig() {
        assertThat(springSecurityFilterChain).isNotNull();
    }

    @Test
    void testCorsConfigurationSource() {
        assertThat(corsConfigurationSource).isNotNull();
    }
    
    private static class ServerWebExchangeMock {
        private final MockServerWebExchange exchange;
        
        ServerWebExchangeMock(MockServerHttpRequest request) {
            this.exchange = mock(MockServerWebExchange.class);
            when(exchange.getRequest()).thenReturn(request);
            when(exchange.getPrincipal()).thenReturn(Mono.error(new RuntimeException("Principal error")));
        }
        
        MockServerWebExchange getExchange() {
            return exchange;
        }
    }
}
