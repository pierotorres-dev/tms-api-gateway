package com.dliriotech.tms.apigateway.security.service;

import com.dliriotech.tms.apigateway.dto.UriRequest;
import com.dliriotech.tms.apigateway.security.cache.TokenValidationCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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

    public Mono<Boolean> validateToken(String token, UriRequest request) {
        // First check if the token validation result is in cache
        return tokenCache.getValidationResult(token)
                .switchIfEmpty(Mono.defer(() -> {
                    // If not in cache, validate with auth-service
                    log.debug("Token not found in cache, validating with auth-service");
                    return validateWithAuthService(token, request)
                            .flatMap(isValid ->
                                    // Cache the result for future requests
                                    tokenCache.cacheValidationResult(token, isValid)
                                            .thenReturn(isValid)
                            );
                }));
    }
    private Mono<Boolean> validateWithAuthService(String token, UriRequest request) {
        long startTime = System.currentTimeMillis();
        return webClient.get()
                .uri("/api/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnSuccess(result -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Token validation with auth-service took {}ms", duration);
                })
                // Implement exponential backoff retry for connection issues
                .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(throwable -> {
                            boolean isConnectionIssue = throwable instanceof PrematureCloseException
                                    || throwable instanceof TimeoutException
                                    || throwable instanceof ConnectException;
                            if (isConnectionIssue) {
                                log.warn("Connection issue detected, retrying: {}", throwable.getMessage());
                            }
                            return isConnectionIssue;
                        })
                        .doAfterRetry(rs -> log.info("Retried connection attempt {} after failure",
                                rs.totalRetries() + 1))
                )
                .timeout(Duration.ofSeconds(10))  // Overall timeout for the operation
                .onErrorResume(error -> {
                    log.error("Error validating token after retries: {}", error.getMessage());
                    return Mono.just(false);
                });
    }
}