package com.truthlens.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        return exchange -> exchange.getPrincipal()
            .map(Principal::getName)
            .switchIfEmpty(Mono.defer(() -> {
                var remoteAddress = exchange.getRequest().getRemoteAddress();
                String ip = (remoteAddress != null && remoteAddress.getAddress() != null)
                        ? remoteAddress.getAddress().getHostAddress()
                        : "anonymous";
                return Mono.just(ip);
            }));
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter, KeyResolver smartKeyResolver) {
        return builder.routes()
            // Backend Verifications Route with Rate Limiting
            .route("backend_verifications_route", r -> r
                .path("/api/v1/verifications/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(smartKeyResolver)))
                .uri("lb://truthlens-backend"))
            // Keycloak Auth Route (Keycloak is not in Eureka, routing directly via Docker host)
            .route("keycloak_auth_route", r -> r
                .path("/api/auth/**")
                .filters(f -> f.rewritePath("/api/auth/?(?<segment>.*)", "/${segment}"))
                .uri("http://truthlens-keycloak:8080"))
            .build();
    }
}
