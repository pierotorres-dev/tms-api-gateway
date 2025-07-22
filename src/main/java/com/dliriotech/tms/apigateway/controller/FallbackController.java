package com.dliriotech.tms.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        log.warn("Fallback triggered for auth-service path: {}", path);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "El servicio de autenticación no está disponible en este momento. Por favor, inténtelo de nuevo.");
        response.put("path", path);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/fleet")
    public Mono<ResponseEntity<Map<String, Object>>> fleetServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        log.warn("Fallback triggered for fleet-service path: {}", path);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "El servicio de flota no está disponible en este momento. Por favor, inténtelo de nuevo.");
        response.put("path", path);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}