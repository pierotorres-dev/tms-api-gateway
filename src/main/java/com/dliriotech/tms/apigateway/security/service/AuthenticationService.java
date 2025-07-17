package com.dliriotech.tms.apigateway.security.service;

import com.dliriotech.tms.apigateway.dto.UriRequest;
import com.dliriotech.tms.apigateway.security.cache.TokenValidationCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                .onErrorReturn(false);
    }
}