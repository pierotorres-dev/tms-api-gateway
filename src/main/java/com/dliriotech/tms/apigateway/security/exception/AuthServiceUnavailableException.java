package com.dliriotech.tms.apigateway.security.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando el auth-service no está disponible o responde
 * con un error de infraestructura (5xx, timeout, conexión rechazada).
 * <p>
 * Se diferencia de un token inválido (401/403): aquí el problema NO es
 * el usuario, sino el servicio de autenticación.
 */
@Getter
public class AuthServiceUnavailableException extends RuntimeException {

    private final HttpStatus status;

    public AuthServiceUnavailableException(String message) {
        super(message);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public AuthServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }
}

