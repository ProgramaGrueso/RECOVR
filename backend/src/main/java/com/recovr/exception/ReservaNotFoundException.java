package com.recovr.exception;

/**
 * Excepción lanzada cuando no se encuentra una reserva con el identificador solicitado.
 */
public class ReservaNotFoundException extends RuntimeException {

    public ReservaNotFoundException(String message) {
        super(message);
    }
}
