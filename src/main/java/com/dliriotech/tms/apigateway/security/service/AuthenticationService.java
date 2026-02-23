package com.dliriotech.tms.apigateway.security.service;

import com.dliriotech.tms.apigateway.dto.TokenValidationResponse;
import com.dliriotech.tms.apigateway.security.cache.TokenValidationCache;
import com.dliriotech.tms.apigateway.security.exception.AuthServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class AuthenticationService {

    private final WebClient webClient;
    private final TokenValidationCache tokenCache;

    public AuthenticationService(WebClient authServiceWebClient, TokenValidationCache tokenCache) {
        this.webClient = authServiceWebClient;
        this.tokenCache = tokenCache;
    }

    public Mono<TokenValidationResponse> validateToken(String token) {
        return tokenCache.getValidationResult(token)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Token not found in cache, validating with auth-service");
                    return validateWithAuthService(token)
                            .flatMap(response ->
                                    tokenCache.cacheValidationResult(token, response)
                                            .thenReturn(response)
                            );
                }));
    }

    private Mono<TokenValidationResponse> validateWithAuthService(String token) {
        long startTime = System.currentTimeMillis();
        return webClient.get()
                .uri("/api/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .doOnSuccess(result -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Token validation with auth-service took {}ms", duration);
                })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(this::isConnectionIssue)
                        .doAfterRetry(rs -> log.info("Retried connection attempt {} after failure",
                                rs.totalRetries() + 1))
                )
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(this::handleValidationError);
    }

    /**
     * Clasifica errores para dar la respuesta correcta al filtro:
     * <ul>
     *   <li>401/403 del auth-service → token inválido/expirado → Mono.empty() (el filtro responde 401/403)</li>
     *   <li>Errores de infraestructura (timeout, 5xx, conexión) → Mono.error (el filtro responde 503)</li>
     * </ul>
     */
    private Mono<TokenValidationResponse> handleValidationError(Throwable error) {
        if (error instanceof WebClientResponseException responseException) {
            HttpStatus status = HttpStatus.valueOf(responseException.getStatusCode().value());

            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                log.warn("Token rechazado por auth-service [{}]: {}",
                        status.value(), responseException.getResponseBodyAsString());
                return Mono.empty();
            }

            log.error("Auth-service respondió con error [{}]: {}",
                    status.value(), responseException.getResponseBodyAsString());
            return Mono.error(new AuthServiceUnavailableException(
                    "Auth-service respondió con error: " + status.value(), error));
        }

        if (isConnectionIssue(error)) {
            log.error("Auth-service no disponible después de reintentos: {}", error.getMessage());
            return Mono.error(new AuthServiceUnavailableException(
                    "Auth-service no disponible: " + error.getMessage(), error));
        }

        log.error("Error inesperado validando token: {}", error.getMessage(), error);
        return Mono.error(new AuthServiceUnavailableException(
                "Error inesperado en la validación: " + error.getMessage(), error));
    }

    private boolean isConnectionIssue(Throwable throwable) {
        boolean isConnectionProblem = throwable instanceof PrematureCloseException
                || throwable instanceof TimeoutException
                || throwable instanceof ConnectException;
        if (isConnectionProblem) {
            log.warn("Connection issue detected: {}", throwable.getMessage());
        }
        return isConnectionProblem;
    }
}