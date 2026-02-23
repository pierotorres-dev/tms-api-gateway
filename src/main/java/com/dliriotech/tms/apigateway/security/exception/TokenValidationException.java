package com.dliriotech.tms.apigateway.security.exception;

/**
 * Excepción lanzada cuando la validación del token indica que el token
 * es inválido o expirado (auth-service retornó vacío / 401).
 * <p>
 * Se usa internamente en el filtro para convertir un Mono.empty() del
 * AuthenticationService en un error que se puede manejar en onErrorResume,
 * evitando el problema de switchIfEmpty sobre Mono&lt;Void&gt; de chain.filter().
 */
public class TokenValidationException extends RuntimeException {

    public TokenValidationException(String message) {
        super(message);
    }
}