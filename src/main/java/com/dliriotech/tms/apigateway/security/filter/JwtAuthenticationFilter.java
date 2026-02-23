package com.dliriotech.tms.apigateway.security.filter;

import com.dliriotech.tms.apigateway.config.PublicRoutesConfig;
import com.dliriotech.tms.apigateway.dto.TokenValidationResponse;
import com.dliriotech.tms.apigateway.error.ErrorHandler;
import com.dliriotech.tms.apigateway.security.exception.AuthServiceUnavailableException;
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

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_EMPRESA_ID = "X-Empresa-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final AuthenticationService authenticationService;
    private final PublicRoutesConfig publicRoutesConfig;
    private final ErrorHandler errorHandler;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();
            String method = request.getMethod().toString();

            // Strip security headers to prevent spoofing from external clients
            ServerWebExchange sanitizedExchange = stripSecurityHeaders(exchange);

            if (publicRoutesConfig.isPublic(path)) {
                log.info("Accediendo a ruta pública: {}", path);
                return chain.filter(sanitizedExchange);
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

            return authenticationService.validateToken(token)
                    .flatMap(validationResponse -> {
                        // Authorization check: verify the role allows this HTTP method
                        if (validationResponse.getAllowedMethods() == null
                                || !validationResponse.getAllowedMethods().contains(method)) {
                            log.warn("Método {} no permitido para rol '{}' en: {}",
                                    method, validationResponse.getRole(), path);
                            return errorHandler.handleAuthError(exchange,
                                    HttpStatus.FORBIDDEN,
                                    String.format("El rol '%s' no tiene permiso para ejecutar %s",
                                            validationResponse.getRole(), method));
                        }

                        log.info("Token validado exitosamente para: {} {} - userId: {}, empresaId: {}, role: {}",
                                method, path,
                                validationResponse.getUserId(),
                                validationResponse.getEmpresaId(),
                                validationResponse.getRole());

                        ServerWebExchange mutatedExchange = addUserContextHeaders(sanitizedExchange, validationResponse);
                        return chain.filter(mutatedExchange);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("Token inválido o expirado para: {} {}", method, path);
                        return errorHandler.handleAuthError(exchange,
                                HttpStatus.UNAUTHORIZED,
                                "Token inválido o expirado");
                    }))
                    .onErrorResume(error -> {
                        if (error instanceof AuthServiceUnavailableException) {
                            log.error("Auth-service no disponible: {}", error.getMessage());
                            return errorHandler.handleAuthError(exchange,
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "Servicio de autenticación no disponible. Intente nuevamente en unos momentos");
                        }
                        log.error("Error inesperado validando token: {}", error.getMessage(), error);
                        return errorHandler.handleAuthError(exchange,
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error interno del servidor");
                    });
        } catch (Exception ex) {
            log.error("Error inesperado en el filtro de autenticación", ex);
            return errorHandler.handleAuthError(exchange,
                    HttpStatus.BAD_REQUEST,
                    "Error en la solicitud: " + ex.getMessage());
        }
    }

    /**
     * Strips security-related headers from the incoming request to prevent clients
     * from spoofing user identity. These headers are only set by the gateway.
     */
    private ServerWebExchange stripSecurityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_EMPRESA_ID);
                    headers.remove(HEADER_USER_ROLE);
                })
                .build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    /**
     * Injects user context from the validated token into the downstream request headers.
     * Downstream services can read X-User-Id, X-Empresa-Id, and X-User-Role
     * instead of relying on path parameters for security-sensitive data.
     */
    private ServerWebExchange addUserContextHeaders(ServerWebExchange exchange, TokenValidationResponse validation) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        if (validation.getUserId() != null) {
            requestBuilder.header(HEADER_USER_ID, validation.getUserId().toString());
        }

        if (validation.getEmpresaId() != null) {
            requestBuilder.header(HEADER_EMPRESA_ID, validation.getEmpresaId().toString());
        }

        if (validation.getRole() != null) {
            requestBuilder.header(HEADER_USER_ROLE, validation.getRole());
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}