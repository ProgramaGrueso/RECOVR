package com.recovr.backend.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de errores de la API: traduce las excepciones de negocio y de validación
 * a respuestas HTTP con un cuerpo JSON uniforme.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        Map<String, Object> body = cuerpo(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revise los campos indicados");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> handleSolicitudMalFormada(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", "El cuerpo o los parámetros de la solicitud no tienen el formato esperado");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ReservaConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictoAgenda(ReservaConflictException ex) {
        return respuesta(HttpStatus.CONFLICT, "Conflicto de Agenda", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleOperacionNoPermitida(IllegalStateException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Operación no permitida", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleSolicitudInvalida(IllegalArgumentException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud Inválida", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoGrande(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "Archivo demasiado grande", "La imagen supera los 3 MB permitidos");
    }

    @ExceptionHandler(LoginBloqueadoException.class)
    public ResponseEntity<Map<String, Object>> handleLoginBloqueado(LoginBloqueadoException ex) {
        return respuesta(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", ex.getMessage());
    }

    @ExceptionHandler(ReservaBloqueadaException.class)
    public ResponseEntity<Map<String, Object>> handleReservaBloqueada(ReservaBloqueadaException ex) {
        return respuesta(HttpStatus.TOO_MANY_REQUESTS, "Reservas bloqueadas temporalmente", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAutenticacion(AuthenticationException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridad(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de la base de datos (registro duplicado o en uso)");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, String mensaje) {
        return ResponseEntity.status(status).body(cuerpo(status, error, mensaje));
    }

    static Map<String, Object> cuerpo(HttpStatus status, String error, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", mensaje);
        return body;
    }
}
