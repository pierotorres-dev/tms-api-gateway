package com.dliriotech.tms.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.PrematureCloseException;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class GatewayRoutingConfig {

    @Value("${service.auth-service-url}")
    private String authServiceUrl;

    @Value("${service.fleet-service-url}")
    private String fleetServiceUrl;

    @Value("${service.tyre-service-url}")
    private String tyreServiceUrl;

    private static final Class<? extends Throwable>[] CONNECTION_EXCEPTIONS = new Class[]{
            PrematureCloseException.class,
            ConnectException.class,
            TimeoutException.class,
            IOException.class
    };

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("authServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/tokens/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("authServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri(authServiceUrl))
                .route("auth-service", r -> r.path("/api/users/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("authServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri(authServiceUrl))
                .route("fleet-service-equipos", r -> r.path("/api/v1/equipos/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("fleetServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/fleet"))
                        )
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/observaciones-equipo/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("fleetServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/fleet"))
                        )
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/catalogos/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("fleetServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/fleet"))
                        )
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/empresas/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("fleetServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/fleet"))
                        )
                        .uri(fleetServiceUrl))
                .route("fleet-service", r -> r.path("/api/v1/tipos-equipos/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                                //.circuitBreaker(config -> config
                                //        .setName("fleetServiceCircuitBreaker")
                                //        .setFallbackUri("forward:/fallback/fleet"))
                        )
                        .uri(fleetServiceUrl))
                .route("tyre-service", r -> r.path("/api/v1/neumaticos/**")
                        .filters(f -> f
                                        .retry(config -> config
                                                .setRetries(3)
                                                .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                                .setExceptions(CONNECTION_EXCEPTIONS)
                                        )
                        )
                        .uri(tyreServiceUrl))
                .route("tyre-service", r -> r.path("/api/v1/observaciones-neumaticos/**")
                        .filters(f -> f
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(300), Duration.ofSeconds(2), 2, true)
                                        .setExceptions(CONNECTION_EXCEPTIONS)
                                )
                        )
                        .uri(tyreServiceUrl))
                .build();
    }
}