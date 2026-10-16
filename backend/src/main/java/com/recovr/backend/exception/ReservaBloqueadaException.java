package com.recovr.backend.exception;

/** El cliente quedó bloqueado temporalmente para reservar por dejar vencer reservas sin pagar. */
public class ReservaBloqueadaException extends RuntimeException {
    public ReservaBloqueadaException(String message) {
        super(message);
    }
}
