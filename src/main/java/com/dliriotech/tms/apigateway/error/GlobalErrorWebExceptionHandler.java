package com.dliriotech.tms.apigateway.error;

import com.dliriotech.tms.apigateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Manejador global de errores para el API Gateway.
 * <p>
 * Captura errores de infraestructura que ocurren cuando los servicios downstream
 * no están disponibles (timeout, conexión rechazada, etc.) y los formatea en un
 * JSON consistente con el formato {@link ErrorResponse}.
 * <p>
 * Los errores de negocio que devuelven los servicios downstream (4xx, 5xx con body)
 * NO pasan por aquí — el gateway los proxea transparentemente al cliente.
 * Este handler solo actúa cuando hay un fallo a nivel de conexión/infraestructura.
 */
@Component
@Order(-1)
@Slf4j
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties.Resources resources,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer configurer) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(configurer.getWriters());
        this.setMessageReaders(configurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        String path = request.path();

        HttpStatus status;
        String message;
        Map<String, String> details = new HashMap<>();

        if (isServiceUnavailable(error)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "El servicio no está disponible en este momento. Intente nuevamente en unos momentos";
            details.put("cause", "downstream_unreachable");
            log.error("Servicio downstream no disponible para [{}]: {}", path, error.getMessage());

        } else if (error instanceof NotFoundException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Servicio no encontrado. Puede estar en proceso de inicio";
            details.put("cause", "service_not_found");
            log.error("Servicio no encontrado para [{}]: {}", path, error.getMessage());

        } else if (error instanceof ResponseStatusException responseStatusEx) {
            status = HttpStatus.valueOf(responseStatusEx.getStatusCode().value());
            message = responseStatusEx.getReason() != null
                    ? responseStatusEx.getReason()
                    : status.getReasonPhrase();
            log.warn("ResponseStatusException en [{}]: {} - {}", path, status.value(), message);

        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del gateway";
            log.error("Error no controlado en gateway para [{}]: {}", path, error.getMessage(), error);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .path(path)
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now(ZoneId.of("America/Lima")))
                .details(details)
                .build();

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorResponse));
    }

    /**
     * Determina si el error es un problema de conectividad con un servicio downstream.
     */
    private boolean isServiceUnavailable(Throwable error) {
        return error instanceof java.io.IOException
                || error instanceof TimeoutException
                || (error.getCause() != null && isServiceUnavailable(error.getCause()));
    }
}



