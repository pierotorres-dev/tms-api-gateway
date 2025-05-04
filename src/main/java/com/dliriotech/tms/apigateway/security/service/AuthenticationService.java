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
    private final String apiGatewayKey;

    public AuthenticationService(WebClient.Builder webClientBuilder,
                                 @Value("${service.api-gateway-key}") String apiGatewayKey) {
        this.webClient = webClientBuilder.build();
        this.apiGatewayKey = apiGatewayKey;
    }

    public Mono<Boolean> validateToken(String token, UriRequest request) {
        return webClient.get()
                .uri("http://auth-service/api/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Service-API-Key", apiGatewayKey)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false);
    }
}