package com.recovr.backend.exception;

/**
 * Se lanza cuando un recurso solicitado por ID no existe en la base de datos (HTTP 404).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " no encontrado con id " + id);
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
