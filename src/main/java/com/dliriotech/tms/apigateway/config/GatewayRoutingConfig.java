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

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/tokens/**")
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/users/**")
                        .uri(authServiceUrl))
                .build();
    }
}