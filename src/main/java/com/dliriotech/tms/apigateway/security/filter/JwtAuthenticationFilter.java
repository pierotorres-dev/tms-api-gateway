package com.dliriotech.tms.apigateway.security.filter;

import com.dliriotech.tms.apigateway.dto.UriRequest;
import com.dliriotech.tms.apigateway.security.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final AuthenticationService authenticationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Verificar si hay header de Authorization
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token de autorización no proporcionado");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Formato de token inválido");
        }

        String token = authHeader.substring(7);
        String path = request.getPath().toString();
        String method = request.getMethod().toString();

        log.info("Validando acceso para: {} {}", method, path);

        return authenticationService.validateToken(token, new UriRequest(path, method))
                .flatMap(valid -> {
                    if (Boolean.TRUE.equals(valid)) {
                        log.info("Token validado exitosamente");
                        return chain.filter(exchange);
                    } else {
                        log.warn("Token inválido o sin permisos");
                        return onError(exchange, HttpStatus.FORBIDDEN, "Sin autorización para acceder a este recurso");
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error validando token: {}", error.getMessage());
                    return onError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Error en la validación");
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}