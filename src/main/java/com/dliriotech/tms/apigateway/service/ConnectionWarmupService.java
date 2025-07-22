package com.dliriotech.tms.apigateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionWarmupService {

    private final WebClient authServiceWebClient;
    private final WebClient fleetServiceWebClient;

    /**
     * Warm up connections on application startup
     */
    @EventListener(ApplicationStartedEvent.class)
    public void warmupOnStartup() {
        log.info("Application started, warming up connections...");
        warmupConnections();
    }

    /**
     * Keep connections warm with periodic health checks
     * Every 60 seconds (60000ms)
     */
    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void scheduledWarmup() {
        log.debug("Running scheduled connection warm-up");
        warmupConnections();
    }

    private void warmupConnections() {
        // Ping auth service
        authServiceWebClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(result -> log.debug("Auth service connection warm-up successful"))
                .onErrorResume(error -> {
                    log.warn("Auth service warm-up failed: {}", error.getMessage());
                    return Mono.empty();
                })
                .subscribe();

        // Ping fleet service
        fleetServiceWebClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(result -> log.debug("Fleet service connection warm-up successful"))
                .onErrorResume(error -> {
                    log.warn("Fleet service warm-up failed: {}", error.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}