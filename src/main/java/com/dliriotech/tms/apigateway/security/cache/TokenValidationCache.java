package com.dliriotech.tms.apigateway.security.cache;

import com.dliriotech.tms.apigateway.dto.TokenValidationResponse;
import reactor.core.publisher.Mono;

public interface TokenValidationCache {
    Mono<TokenValidationResponse> getValidationResult(String token);
    Mono<Void> cacheValidationResult(String token, TokenValidationResponse response);
}