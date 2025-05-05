package com.dliriotech.tms.apigateway.error;

import com.dliriotech.tms.apigateway.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ErrorHandler {

    private final ObjectMapper objectMapper;

    public Mono<Void> handleError(ServerWebExchange exchange, HttpStatus status,
                                  String message, Map<String, String> details) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String traceId = exchange.getAttribute("traceId");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .path(path)
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .details(details != null ? details : new HashMap<>())
                .build();

        try {
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            log.warn("Error en API Gateway: {} - {}", status.value(), message);

            DataBuffer buffer = response.bufferFactory()
                    .wrap(errorJson.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error al serializar respuesta de error", e);
            return response.setComplete();
        }
    }

    public Mono<Void> handleAuthError(ServerWebExchange exchange, HttpStatus status, String message) {
        Map<String, String> details = new HashMap<>();
        details.put("service", "authentication");
        details.put("scope", exchange.getRequest().getPath().toString());

        return handleError(exchange, status, message, details);
    }
}