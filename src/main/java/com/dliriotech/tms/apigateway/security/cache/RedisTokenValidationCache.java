package com.dliriotech.tms.apigateway.security.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTokenValidationCache implements TokenValidationCache {

    @Value("${app.cache.ttl-seconds}")
    private long cacheTtlSeconds;
    @Value("${app.cache.prefixes.token-validation}")
    private String cachePrefix;

    private final ReactiveRedisTemplate<String, Boolean> redisTemplate;

    @Override
    public Mono<Boolean> getValidationResult(String token) {
        String key = createKey(token);
        return redisTemplate.opsForValue().get(key)
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.debug("Token validation result found in cache: {}", key);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error retrieving token validation from cache: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> cacheValidationResult(String token, boolean isValid) {
        String key = createKey(token);
        Duration cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        return redisTemplate.opsForValue().set(key, isValid, cacheTtl)
                .doOnSuccess(result -> log.debug("Token validation result cached: {}", key))
                .onErrorResume(e -> {
                    log.error("Error caching token validation: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private String createKey(String token) {
        // Using hash to avoid storing the actual token
        return cachePrefix + Integer.toHexString(token.hashCode());
    }
}
