package com.recovr.exception;

/**
 * Excepción de negocio lanzada cuando existe un conflicto de agenda
 * (solapamiento de horario de especialista o de sala de tratamiento).
 */
public class ReservaConflictException extends RuntimeException {

    public ReservaConflictException(String message) {
        super(message);
    }
}
