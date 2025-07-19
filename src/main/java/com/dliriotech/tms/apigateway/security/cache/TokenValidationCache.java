package com.dliriotech.tms.apigateway.security.cache;

import reactor.core.publisher.Mono;

public interface TokenValidationCache {
    Mono<Boolean> getValidationResult(String token);
    Mono<Void> cacheValidationResult(String token, boolean isValid);
}