package com.recovr.backend.exception;

/** Demasiados intentos de inicio de sesión fallidos: la cuenta queda bloqueada unos minutos (HTTP 429). */
public class LoginBloqueadoException extends RuntimeException {
    public LoginBloqueadoException(String mensaje) {
        super(mensaje);
    }
}
