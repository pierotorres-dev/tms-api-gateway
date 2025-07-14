package com.dliriotech.tms.apigateway.security.service;

import com.dliriotech.tms.apigateway.dto.UriRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthenticationService {

    private final WebClient webClient;

    public AuthenticationService(WebClient authServiceWebClient) {
        this.webClient = authServiceWebClient;
    }

    public Mono<Boolean> validateToken(String token, UriRequest request) {
        return webClient.get()
                .uri("/api/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false);
    }
}