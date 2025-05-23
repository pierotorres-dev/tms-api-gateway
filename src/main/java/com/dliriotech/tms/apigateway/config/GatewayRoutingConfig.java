package com.dliriotech.tms.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/tokens/**")
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/users/**")
                        .uri(authServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/equipos/**")
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/observaciones-equipo/**")
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/catalogos/**")
                        .uri(fleetServiceUrl))
                .build();
    }
}