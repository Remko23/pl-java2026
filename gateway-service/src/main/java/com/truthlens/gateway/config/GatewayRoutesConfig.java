package com.truthlens.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class GatewayRoutesConfig {

    @Value("${truthlens.gateway.rate-limit.replenish-rate:5}")
    private int replenishRate;

    @Value("${truthlens.gateway.rate-limit.burst-capacity:10}")
    private int burstCapacity;

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    @Bean
    public KeyResolver smartKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return exchange.getPrincipal()
                        .map(Principal::getName)
                        .onErrorResume(e -> Mono.just(fallbackKey(exchange)));
            }
            return Mono.just(fallbackKey(exchange));
        };
    }

    private String fallbackKey(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return (remoteAddress != null && remoteAddress.getAddress() != null)
                ? remoteAddress.getAddress().getHostAddress()
                : "anonymous";
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter,
            KeyResolver smartKeyResolver) {
        return builder.routes()
                .route("backend_verifications_route", r -> r
                        .path("/api/v1/verifications/**")
                        .filters(f -> f
                                .tokenRelay()
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(smartKeyResolver)))
                        .uri("lb://truthlens-backend"))
                .route("backend_history_route", r -> r
                        .path("/api/v1/history/**")
                        .filters(f -> f
                                .tokenRelay()
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(smartKeyResolver)))
                        .uri("lb://truthlens-backend"))
                .route("backend_health_route", r -> r
                        .path("/api/v1/health")
                        .uri("lb://truthlens-backend"))
                .route("backend_users_route", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .tokenRelay()
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(smartKeyResolver)))
                        .uri("lb://truthlens-backend"))
                .route("keycloak_auth_route", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.rewritePath("/api/auth/?(?<segment>.*)", "/${segment}"))
                        .uri("http://truthlens-keycloak:8080"))
                .route("search_internal_route", r -> r
                        .path("/api/internal/v1/search**", "/api/internal/v1/search/**")
                        .uri("lb://truthlens-search-service"))
                .route("ocr_internal_route", r -> r
                        .path("/api/internal/v1/ocr**", "/api/internal/v1/ocr/**")
                        .uri("lb://truthlens-ocr-service"))
                .route("openapi_backend", r -> r
                        .path("/aggregate/backend/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/backend/(?<segment>.*)", "/${segment}"))
                        .uri("lb://truthlens-backend"))
                .route("openapi_ocr", r -> r
                        .path("/aggregate/ocr/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/ocr/(?<segment>.*)", "/${segment}"))
                        .uri("lb://truthlens-ocr-service"))
                .route("openapi_search", r -> r
                        .path("/aggregate/search/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/search/(?<segment>.*)", "/${segment}"))
                        .uri("lb://truthlens-search-service"))
                .build();
    }
}
