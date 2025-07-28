package com.dliriotech.tms.apigateway.security.filter;

import com.dliriotech.tms.apigateway.config.PublicRoutesConfig;
import com.dliriotech.tms.apigateway.dto.UriRequest;
import com.dliriotech.tms.apigateway.error.ErrorHandler;
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
    private final PublicRoutesConfig publicRoutesConfig;
    private final ErrorHandler errorHandler;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();
            String method = request.getMethod().toString();

            if (publicRoutesConfig.isPublic(path)) {
                log.info("Accediendo a ruta pública: {}", path);
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return errorHandler.handleAuthError(exchange,
                        HttpStatus.UNAUTHORIZED,
                        "Se requiere token de autorización para acceder a " + path);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return errorHandler.handleAuthError(exchange,
                        HttpStatus.UNAUTHORIZED,
                        "Formato de token inválido. Se espera 'Bearer {token}'");
            }

            String token = authHeader.substring(7);
            log.info("Validando acceso para: {} {}", method, path);

            return authenticationService.validateToken(token, new UriRequest(path, method))
                    .flatMap(valid -> {
                        if (Boolean.TRUE.equals(valid)) {
                            log.info("Token validado exitosamente para: {} {}", method, path);
                            return chain.filter(exchange);
                        } else {
                            String serviceName = extractServiceName(path);
                            return errorHandler.handleAuthError(exchange,
                                    HttpStatus.FORBIDDEN,
                                    "El token no tiene autorización para acceder al servicio: " + serviceName);
                        }
                    })
                    .onErrorResume(error -> {
                        log.error("Error validando token: {}", error.getMessage(), error);
                        return errorHandler.handleAuthError(exchange,
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error en el servicio de autenticación: " + error.getMessage());
                    });
        } catch (Exception ex) {
            log.error("Error inesperado en el filtro de autenticación", ex);
            return errorHandler.handleAuthError(exchange,
                    HttpStatus.BAD_REQUEST,
                    "Error en la solicitud: " + ex.getMessage());
        }
    }

    private String extractServiceName(String path) {
        try {
            String[] segments = path.split("/");
            if (segments.length > 2) {
                return segments[2];
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer el nombre del servicio del path: {}", path);
        }
        return "desconocido";
    }

    @Override
    public int getOrder() {
        return -1;
    }
}