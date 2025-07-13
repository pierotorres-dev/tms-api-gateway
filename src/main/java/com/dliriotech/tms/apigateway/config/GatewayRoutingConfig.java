package com.dliriotech.tms.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayRoutingConfig {

    @Value("${service.auth-service-url}")
    private String authServiceUrl;

    @Value("${service.fleet-service-url}")
    private String fleetServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.retry(config -> config
                                .setRetries(3)
                                .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)))
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/tokens/**")
                        .filters(f -> f.retry(config -> config
                                .setRetries(3)
                                .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)))
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/users/**")
                        .filters(f -> f.retry(config -> config
                                .setRetries(3)
                                .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)))
                        .uri(authServiceUrl))
                .route("fleet-service-equipos", r -> r.path("/api/v1/equipos/**")
                        .filters(f -> f.retry(config -> config
                                .setRetries(3)
                                .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)))
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/observaciones-equipo/**")
                        .filters(f -> f.retry(config -> config
                                .setRetries(3)
                                .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)))
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/catalogos/**")
                        .filters(f -> f.retry(config -> config
                                .setRetries(3)
                                .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)))
                        .uri(fleetServiceUrl))
                .build();
    }
}