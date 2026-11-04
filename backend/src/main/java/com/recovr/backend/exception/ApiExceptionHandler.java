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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de errores de la API: traduce las excepciones de negocio y de validación
 * a respuestas HTTP con un cuerpo JSON uniforme.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    /**
     * Dos operaciones compitieron por la misma fila (p. ej. MySQL "Lock wait timeout"): no es un fallo del servidor
     * sino un conflicto momentáneo, y el cliente puede reintentar.
     */
    @ExceptionHandler({org.springframework.dao.PessimisticLockingFailureException.class,
            org.springframework.dao.ConcurrencyFailureException.class})
    public ResponseEntity<Map<String, Object>> handleBloqueo(Exception ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getClass().getSimpleName());
        return respuesta(HttpStatus.CONFLICT, "Recurso ocupado",
                "Otra operación está usando el mismo horario en este momento. Intente de nuevo.");
    }

    /**
     * Último recurso: los errores propios de Spring MVC (404 de ruta, 405, 415…) conservan su código; cualquier otra
     * excepción se registra en el log y responde 500 con un mensaje genérico, sin clases ni mensajes internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInesperado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return respuesta(status, status.getReasonPhrase(), "La solicitud no se puede atender: " + status.getReasonPhrase());
            }
        }
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Si persiste, comuníquese con el administrador.");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, String mensaje) {
        return ResponseEntity.status(status).body(cuerpo(status, error, mensaje));
    }

    static Map<String, Object> cuerpo(HttpStatus status, String error, String mensaje) {
        return CuerpoError.de(status, error, mensaje);
    }
}
